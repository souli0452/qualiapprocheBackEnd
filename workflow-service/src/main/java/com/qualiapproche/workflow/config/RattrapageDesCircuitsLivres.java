package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Complète les circuits déjà en base en étapes et en transitions.
 *
 * <p>{@link WorkflowDataInitializer} ne crée un circuit que si aucun n'existe pour la famille de
 * ressources : sur une installation en service — c'est-à-dire toutes — un circuit enrichi après
 * coup n'arrivait jamais jusqu'aux bases existantes. Une étape ajoutée au circuit livré, comme la
 * validation qualité qui oriente le dossier, restait donc invisible partout sauf sur une base
 * vierge, et la seule issue était de supprimer le circuit — c'est-à-dire de perdre l'historique de
 * tous les dossiers qu'il pilote.</p>
 *
 * <p><b>Ce qui est ajouté, et ce qui ne l'est pas.</b> Le rattrapage n'ajoute que ce qui manque :
 * une étape dont le code est absent, une transition dont la décision n'est pas déjà émise depuis
 * son étape. Il ne réécrit jamais une étape ni une transition existante — un administrateur a pu
 * en changer le libellé, la couleur ou l'habilitation, et ce n'est pas à un rattrapage de défaire
 * son choix.</p>
 *
 * <p><b>Ce qu'il refuse de toucher.</b> Un circuit qui porte une étape inconnue du circuit livré a
 * été recomposé à la main : y greffer des étapes reviendrait à mêler deux conceptions, et le
 * résultat ne serait celui de personne. Ces circuits sont signalés dans le journal et laissés
 * intacts, à l'administrateur de les compléter depuis l'éditeur.</p>
 *
 * <p>Les dossiers en cours ne sont pas touchés : ils désignent leur étape courante par son
 * identifiant, qu'aucun ajout ne déplace. Un dossier arrêté à une étape que le circuit livré fait
 * désormais suivre d'une nouvelle étape empruntera la nouvelle route à sa prochaine décision — ce
 * qui est précisément l'effet recherché.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(120) // après ChampDocumentRejetInitializer, qui complète les champs
public class RattrapageDesCircuitsLivres implements CommandLineRunner {

    /**
     * Champs qu'une étape du circuit livré a demandés puis abandonnés, par code d'étape.
     *
     * <p>Le traitement décrivait en texte libre ce que portent les plans d'action ; la validation
     * demandait au pilote d'écrire que l'action est pertinente, ce que sa décision dit déjà et que
     * son commentaire justifie.</p>
     */
    private static final Map<String, List<String>> CHAMPS_ABANDONNES = Map.of(
            "TRAITEMENT", List.of("actionPreventive", "delaisMiseOeuvre", "actionDsc"),
            "VALIDATION", List.of("pertinancePilote", "justificationPilote"));

    /**
     * Champs qu'une étape exigeait et n'exige plus, par code d'étape.
     *
     * <p>Différent d'un abandon : le champ reste offert, il cesse seulement d'être obligatoire. La
     * cause et la solution retenue sont désormais posées à la <b>proposition</b> du plan d'action,
     * par la personne imputée ; les redemander au responsable qui réalise l'action reviendrait à
     * faire ressaisir ce que le dossier porte déjà, et — les champs étant obligatoires — à bloquer
     * sa décision tant qu'il ne l'aurait pas fait.</p>
     *
     * <p>Un rattrapage qui ne complète que ce qui manque ne pouvait pas produire cet effet : le
     * champ existe en base, avec son ancienne exigence, et rien ne serait venu la lever.</p>
     */
    private static final Map<String, List<String>> CHAMPS_DEVENUS_FACULTATIFS = Map.of(
            "NON_TRAITER", List.of("causeIdentifiees", "solutionRetenues"));

    /**
     * Gabarits de courriel livrés avant que chaque étape n'ait le sien.
     *
     * <p>Une poignée de modèles génériques servait à toutes les étapes : celle de la soumission
     * annonçait « Nouvelle non-conformité imputée », celle de la clôture « Non-conformité transmise
     * ». Chaque étape a maintenant son message, qui dit à son destinataire ce qu'on attend
     * précisément de lui.</p>
     *
     * <p>Le remplacement ne vise que ces anciens codes : un gabarit choisi par un administrateur
     * n'en fait pas partie et n'est jamais touché. Sans cette liste, il aurait fallu ou bien tout
     * écraser — y compris son travail — ou bien ne rien reprendre, et les installations en service
     * auraient gardé les messages génériques que le client demande justement de remplacer.</p>
     */
    private static final java.util.Set<String> ANCIENS_GABARITS = java.util.Set.of(
            "emailTemplate", "structureToStructure", "validationNonConformite", "validationRq",
            "emailPlanAction", "validationPlanRequise", "emailRqPlan", "validationAfterPlan",
            "succesTraitementNonformite", "traitementReussi", "rejectNonConformite");

    /**
     * Actions dont le circuit livré a changé le code, par code d'étape puis ancien code.
     *
     * <p>Une action n'avait pas de nom propre tant qu'une étape n'en offrait que deux : son code
     * valait celui de sa décision, et {@code RattrapageDesActionsDEtape} a nommé « APPROUVE »
     * celles des bases en service. Dès qu'une étape en offre deux qui approuvent, il leur faut
     * chacune un nom.</p>
     *
     * <p>Le renommage doit précéder l'ajout des actions manquantes, et c'est tout son intérêt :
     * sans lui, l'action livrée sous son nouveau nom serait vue comme absente et <b>ajoutée</b>, le
     * dossier se retrouvant avec deux boutons menant au même endroit, dont un seul réclamerait les
     * saisies attendues.</p>
     */
    private static final Map<String, Map<String, String>> ACTIONS_RENOMMEES = Map.of(
            "VALIDATION_RQ", Map.of("APPROUVE", "VALIDER_ET_ORIENTER"));

    private final WorkflowRepository workflowRepository;

    @Override
    @Transactional
    public void run(String... args) {
        completer("NON_CONFORMITE", WorkflowDataInitializer::circuitNonConformiteParDefaut);
        completer("PLAN_ACTION", WorkflowDataInitializer::circuitPlanActionParDefaut);
        // Les deux circuits documentaires ne reçoivent que l'adressage de leurs courriels — voir
        // adresserSeulement.
        adresserSeulement("DOCUMENT", WorkflowDataInitializer::circuitDocumentParDefaut);
        adresserSeulement("DEMANDE_DOCUMENT", WorkflowDataInitializer::circuitDemandeDocumentParDefaut);
    }

    /**
     * Rattrape les seuls courriels d'un circuit, sans toucher à ses étapes ni à ses routes.
     *
     * <p>Les circuits documentaires empruntaient les gabarits génériques des non-conformités : le
     * rédacteur d'une procédure recevait « Nouvelle Non-Conformité imputée », et le responsable
     * qualité à qui un document était transmis pour approbation, « Validation attendue -
     * Non-Conformité ». {@link WorkflowDataInitializer} ne recréant les circuits que sur une base
     * vierge, aucune installation en service n'aurait vu les gabarits neufs.</p>
     *
     * <p>Rattrapage <b>restreint</b>, et c'est délibéré : ces deux circuits n'ont pas changé de
     * forme, et leur appliquer le rattrapage complet y rajouterait les étapes, routes et champs du
     * circuit livré — c'est-à-dire défaire ce qu'un administrateur a pu retirer en connaissance de
     * cause. Seul l'adressage est repris, et lui-même ne remplace qu'un ancien gabarit générique :
     * un modèle choisi depuis l'écran d'administration reste en place.</p>
     */
    private void adresserSeulement(String typeRessource, Supplier<Workflow> circuitDeReference) {
        List<Workflow> circuits = workflowRepository.findByResourceType(typeRessource);
        if (circuits.isEmpty()) {
            return;
        }

        Map<String, WorkflowStep> etapesDeReference = parCode(circuitDeReference.get().getSteps());
        for (Workflow circuit : circuits) {
            int adressees = adresserLesCourriels(circuit, etapesDeReference);
            if (adressees > 0) {
                workflowRepository.save(circuit);
                log.info("Circuit « {} » : {} courriel(s) d'étape réadressé(s).",
                        circuit.getNom(), adressees);
            }
        }
    }

    private void completer(String typeRessource, Supplier<Workflow> circuitDeReference) {
        List<Workflow> circuits = workflowRepository.findByResourceType(typeRessource);
        if (circuits.isEmpty()) {
            return;
        }

        Workflow reference = circuitDeReference.get();
        Map<String, WorkflowStep> etapesDeReference = parCode(reference.getSteps());

        for (Workflow circuit : circuits) {
            if (aEteRecompose(circuit, etapesDeReference)) {
                log.info("Circuit « {} » laissé intact : il porte des étapes qui ne sont pas celles du "
                        + "circuit livré. À compléter depuis l'éditeur si besoin.", circuit.getNom());
                continue;
            }

            int ajouts = renommerLesActions(circuit)
                    + ajouterLesEtapesManquantes(circuit, reference, etapesDeReference)
                    + ajouterLesTransitionsManquantes(circuit, etapesDeReference)
                    + ajouterLesChampsManquants(circuit, etapesDeReference)
                    + retirerLesChampsAbandonnes(circuit)
                    + libererLesChampsDevenusFacultatifs(circuit)
                    + rattacherLesChampsALeurAction(circuit, etapesDeReference)
                    + adresserLesCourriels(circuit, etapesDeReference)
                    + reserverAuTitulaire(circuit, etapesDeReference);
            if (ajouts > 0) {
                workflowRepository.save(circuit);
                log.info("Circuit « {} » complété : {} ajout(s) d'étape ou de transition.",
                        circuit.getNom(), ajouts);
            }
        }
    }

    /**
     * Un circuit porte-t-il une étape que le circuit livré ne connaît pas ?
     *
     * <p>Une étape sans code en fait partie : elle a été créée par l'éditeur avant que les codes
     * n'existent, et rien ne permet de l'apparier à une étape de référence.</p>
     */
    private boolean aEteRecompose(Workflow circuit, Map<String, WorkflowStep> etapesDeReference) {
        return circuit.getSteps().stream()
                .anyMatch(step -> step.getCode() == null || !etapesDeReference.containsKey(step.getCode()));
    }

    private int ajouterLesEtapesManquantes(Workflow circuit, Workflow reference,
            Map<String, WorkflowStep> etapesDeReference) {
        Map<String, WorkflowStep> presentes = parCode(circuit.getSteps());
        int ajoutees = 0;

        for (WorkflowStep modele : reference.getSteps()) {
            if (presentes.containsKey(modele.getCode())) {
                continue;
            }
            circuit.addStep(copierEtape(modele));
            ajoutees++;
            log.info("Circuit « {} » : étape « {} » ajoutée.", circuit.getNom(), modele.getCode());
        }

        if (ajoutees > 0) {
            // Les rangs du circuit livré font autorité, sans quoi une étape insérée au milieu
            // s'afficherait en fin de parcours et l'ordre lu ne serait celui de personne.
            circuit.getSteps().forEach(step -> {
                WorkflowStep modele = etapesDeReference.get(step.getCode());
                if (modele != null) {
                    step.setStepOrder(modele.getStepOrder());
                }
            });
            // Les étapes neuves doivent être enregistrées avant qu'une route ne les vise. Sans ce
            // flush, l'enregistrement du circuit passait par un merge qui persistait des copies des
            // étapes, tandis que les transitions gardaient la main sur les originales, jamais
            // enregistrées : le démarrage échouait en TransientPropertyValueException sur toute
            // base dont le circuit avait des étapes en retard — et, la transaction n'aboutissant
            // jamais, échouait à l'identique à chaque redémarrage.
            workflowRepository.flush();
            ajoutees += alignerLesDestinations(circuit, etapesDeReference);
        }
        return ajoutees;
    }

    /**
     * Fait passer les routes existantes par les étapes qui viennent d'être ajoutées.
     *
     * <p>Une étape insérée au milieu du parcours ne sert à rien si les transitions qui l'encadrent
     * continuent de l'enjamber : la validation qualité aurait été ajoutée au circuit, visible dans
     * l'éditeur, et jamais atteinte par un seul dossier. Une route n'est pas une préférence
     * d'affichage — c'est la structure même du circuit.</p>
     *
     * <p>N'a lieu que sur un circuit à qui il manquait des étapes, donc structurellement en retard
     * sur le circuit livré. Un circuit déjà complet garde ses routes telles que l'administrateur
     * les a posées.</p>
     */
    private int alignerLesDestinations(Workflow circuit, Map<String, WorkflowStep> etapesDeReference) {
        Map<String, WorkflowStep> presentes = parCode(circuit.getSteps());
        int alignees = 0;

        for (WorkflowStep step : circuit.getSteps()) {
            WorkflowStep modele = etapesDeReference.get(step.getCode());
            if (modele == null) {
                continue;
            }

            for (WorkflowTransition transition : step.getTransitions()) {
                // Appariées par code d'action, et non par décision : une étape peut offrir
                // plusieurs suites qui approuvent, et la décision ne suffit plus à les distinguer.
                WorkflowTransition transitionDeReference = modele.getTransitions().stream()
                        .filter(t -> Objects.equals(t.codeEffectif(), transition.codeEffectif()))
                        .findFirst().orElse(null);
                if (transitionDeReference == null) {
                    continue;
                }
                WorkflowStep attendue = destination(transitionDeReference, presentes);
                if (attendue == null || attendue.equals(transition.getToStep())) {
                    continue;
                }
                String ancienne = transition.getToStep() == null ? "fin de circuit" : transition.getToStep().getCode();
                transition.setToStep(attendue);
                transition.setTerminal(transitionDeReference.isTerminal());
                alignees++;
                log.info("Circuit « {} » : la décision {} de l'étape « {} » mène désormais à « {} » et non « {} ».",
                        circuit.getNom(), transition.getDecision(), step.getCode(), attendue.getCode(), ancienne);
            }
        }
        return alignees;
    }

    private int ajouterLesTransitionsManquantes(Workflow circuit, Map<String, WorkflowStep> etapesDeReference) {
        Map<String, WorkflowStep> presentes = parCode(circuit.getSteps());
        int ajoutees = 0;

        for (WorkflowStep step : circuit.getSteps()) {
            WorkflowStep modele = etapesDeReference.get(step.getCode());
            if (modele == null) {
                continue;
            }

            for (WorkflowTransition transitionDeReference : modele.getTransitions()) {
                if (emetDeja(step, transitionDeReference.codeEffectif())) {
                    continue;
                }
                WorkflowStep destination = destination(transitionDeReference, presentes);
                if (destination == null && !transitionDeReference.isTerminal()) {
                    // La destination n'existe pas encore dans ce circuit : la transition serait
                    // prise pour une fin de circuit. Mieux vaut ne rien poser.
                    continue;
                }
                step.getTransitions().add(copierTransition(transitionDeReference, step, destination));
                ajoutees++;
                log.info("Circuit « {} » : décision {} ajoutée à l'étape « {} ».",
                        circuit.getNom(), transitionDeReference.getDecision(), step.getCode());
            }
        }
        return ajoutees;
    }

    /**
     * Pose sur les étapes existantes les champs que le circuit livré leur fait porter.
     *
     * <p>Un champ ajouté à une étape déjà en base n'atteignait aucune installation en service : le
     * compte rendu que le circuit livré demande au responsable de l'action n'aurait été recueilli
     * que sur une base vierge, et partout ailleurs l'étape serait restée sans point de saisie — la
     * décision serait passée, et ce qu'elle devait justifier ne serait allé nulle part.</p>
     *
     * <p>Un champ déjà porté par l'étape, sous quelque forme que ce soit, est laissé tel quel :
     * l'administrateur a pu en changer le libellé, le type ou la portée, et ce n'est pas à un
     * rattrapage de défaire son choix.</p>
     */
    private int ajouterLesChampsManquants(Workflow circuit, Map<String, WorkflowStep> etapesDeReference) {
        int ajoutes = 0;

        for (WorkflowStep step : circuit.getSteps()) {
            WorkflowStep modele = etapesDeReference.get(step.getCode());
            if (modele == null) {
                continue;
            }

            for (WorkflowStepField champ : modele.getFields()) {
                if (porteLeChamp(step, champ.getFieldName())) {
                    continue;
                }
                step.getFields().add(WorkflowStepField.builder()
                        .step(step)
                        .fieldName(champ.getFieldName())
                        .fieldLabel(champ.getFieldLabel())
                        .type(champ.getType())
                        .options(champ.getOptions())
                        .decision(champ.getDecision())
                        .actionCode(champ.getActionCode())
                        .isRequired(champ.isRequired())
                        .build());
                ajoutes++;
                log.info("Circuit « {} » : champ « {} » ajouté à l'étape « {} ».",
                        circuit.getNom(), champ.getFieldName(), step.getCode());
            }
        }
        return ajoutes;
    }

    /**
     * Retire des étapes les champs que le circuit livré ne demande plus.
     *
     * <p>Le rattrapage ne complète que ce qui manque : un champ retiré du circuit livré restait
     * donc en place sur toutes les bases en service, et, la plupart étant obligatoires, continuait
     * d'être <b>exigé</b> à chaque décision. Un champ abandonné ne se laisse pas mourir de sa
     * belle mort.</p>
     *
     * <p>C'est un retrait nommé, non une remise à l'identique : seuls les champs de cette liste
     * disparaissent, et un champ ajouté par un administrateur depuis l'éditeur n'est jamais touché.
     * Les valeurs déjà saisies restent dans l'historique des décisions, qui ne dépend pas du
     * circuit.</p>
     */
    private int retirerLesChampsAbandonnes(Workflow circuit) {
        int retires = 0;

        for (WorkflowStep step : circuit.getSteps()) {
            List<String> abandonnes = CHAMPS_ABANDONNES.getOrDefault(step.getCode(), List.of());
            if (abandonnes.isEmpty()) {
                continue;
            }
            for (String nomDeChamp : abandonnes) {
                if (step.getFields().removeIf(champ -> nomDeChamp.equals(champ.getFieldName()))) {
                    retires++;
                    log.info("Circuit « {} » : champ « {} » retiré de l'étape « {} ».",
                            circuit.getNom(), nomDeChamp, step.getCode());
                }
            }
        }
        return retires;
    }

    /**
     * Donne leur nom propre aux actions que le circuit livré a renommées.
     *
     * <p>Premier geste du rattrapage, et il conditionne tous les suivants : une action est
     * reconnue par son code, et une action renommée passerait sinon pour absente — donc ajoutée en
     * double, à côté de celle qu'elle était censée devenir.</p>
     *
     * <p>Seul le code change. Le libellé, l'icône, la couleur et la destination restent ceux que
     * l'administrateur a posés : ce n'est pas un alignement sur le circuit livré, c'est une mise à
     * jour d'identifiant.</p>
     */
    private int renommerLesActions(Workflow circuit) {
        int renommees = 0;

        for (WorkflowStep step : circuit.getSteps()) {
            Map<String, String> renommages = ACTIONS_RENOMMEES.getOrDefault(step.getCode(), Map.of());
            if (renommages.isEmpty()) {
                continue;
            }
            for (WorkflowTransition transition : step.getTransitions()) {
                String nouveauCode = renommages.get(transition.codeEffectif());
                if (nouveauCode == null || emetDeja(step, nouveauCode)) {
                    continue;
                }
                transition.setCode(nouveauCode);
                renommees++;
                log.info("Circuit « {} » : l'action « {} » de l'étape « {} » s'appelle désormais « {} ».",
                        circuit.getNom(), transition.getLabel(), step.getCode(), nouveauCode);
            }
        }
        return renommees;
    }

    /**
     * Lève l'obligation sur les champs que le circuit livré n'exige plus.
     *
     * <p>Le champ reste en place : ce n'est pas un abandon, c'est un déplacement du moment où on le
     * demande. La cause et la solution retenue se saisissent désormais à la proposition du plan
     * d'action ; laissées obligatoires à l'étape de réalisation, elles auraient empêché le
     * responsable de déclarer son action faite tant qu'il n'aurait pas recopié ce que le dossier
     * portait déjà.</p>
     */
    private int libererLesChampsDevenusFacultatifs(Workflow circuit) {
        int liberes = 0;

        for (WorkflowStep step : circuit.getSteps()) {
            List<String> facultatifs = CHAMPS_DEVENUS_FACULTATIFS.getOrDefault(step.getCode(), List.of());
            for (WorkflowStepField champ : step.getFields()) {
                if (!facultatifs.contains(champ.getFieldName()) || !champ.isRequired()) {
                    continue;
                }
                champ.setRequired(false);
                liberes++;
                log.info("Circuit « {} » : le champ « {} » de l'étape « {} » n'est plus obligatoire.",
                        circuit.getNom(), champ.getFieldName(), step.getCode());
            }
        }
        return liberes;
    }

    /**
     * Rattache à leur action les champs que le circuit livré ne demande qu'à l'une des issues.
     *
     * <p>Ce n'est pas une préférence de configuration mais une condition pour que l'étape reste
     * franchissable. La validation qualité offre désormais deux issues qui approuvent — orienter le
     * dossier, ou le clore sans suite ; un champ obligatoire sans portée, comme le processus
     * destinataire, serait exigé des <b>deux</b>, et il deviendrait impossible de clore sans
     * désigner à qui l'on transmet un dossier qu'on vient de décider de ne transmettre à personne.
     * Un champ qui porte déjà une action n'est pas touché.</p>
     */
    private int rattacherLesChampsALeurAction(Workflow circuit, Map<String, WorkflowStep> etapesDeReference) {
        int rattaches = 0;

        for (WorkflowStep step : circuit.getSteps()) {
            WorkflowStep modele = etapesDeReference.get(step.getCode());
            if (modele == null) {
                continue;
            }
            for (WorkflowStepField champ : step.getFields()) {
                if (champ.getActionCode() != null && !champ.getActionCode().isBlank()) {
                    continue;
                }
                String actionDeReference = modele.getFields().stream()
                        .filter(reference -> reference.getFieldName().equals(champ.getFieldName()))
                        .map(WorkflowStepField::getActionCode)
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
                if (actionDeReference == null) {
                    continue;
                }
                champ.setActionCode(actionDeReference);
                rattaches++;
                log.info("Circuit « {} » : le champ « {} » de l'étape « {} » n'est plus demandé qu'à "
                        + "l'action « {} ».", circuit.getNom(), champ.getFieldName(), step.getCode(),
                        actionDeReference);
            }
        }
        return rattaches;
    }

    /**
     * Donne aux étapes le gabarit de courriel que le circuit livré leur associe, et son destinataire.
     *
     * <p>Une poignée de modèles génériques servait à toutes les étapes, et disait donc à peu près
     * n'importe quoi : l'agent dont la déclaration revenait pour correction recevait « Nouvelle
     * non-conformité imputée ». Chaque étape a maintenant son message. Le remplacement ne vise que
     * les anciens codes livrés — un gabarit qu'un administrateur a choisi lui-même n'en fait pas
     * partie, et reste en place.</p>
     *
     * <p>La désignation du destinataire suit la même logique, et elle n'est jamais défaite : sans
     * elle, la clôture d'une non-conformité serait annoncée au responsable qualité qui vient de la
     * prononcer, et non au pilote du processus qui avait signalé l'écart et qui attend d'apprendre
     * ce qu'il est devenu.</p>
     */
    private int adresserLesCourriels(Workflow circuit, Map<String, WorkflowStep> etapesDeReference) {
        int adressees = 0;

        for (WorkflowStep step : circuit.getSteps()) {
            WorkflowStep modele = etapesDeReference.get(step.getCode());
            if (modele == null) {
                continue;
            }

            String gabaritLivre = modele.getEmailTemplateCode();
            String gabaritEnPlace = step.getEmailTemplateCode();
            boolean aRemplacer = gabaritLivre != null && !gabaritLivre.equals(gabaritEnPlace)
                    && (gabaritEnPlace == null || gabaritEnPlace.isBlank()
                            || ANCIENS_GABARITS.contains(gabaritEnPlace));
            if (aRemplacer) {
                step.setEmailTemplateCode(gabaritLivre);
                adressees++;
                log.info("Circuit « {} » : l'étape « {} » annonce désormais son franchissement par le "
                        + "gabarit « {} » et non « {} ».", circuit.getNom(), step.getCode(),
                        gabaritLivre, gabaritEnPlace);
            }

            if (modele.getDestinataireCourriel() != null && step.getDestinataireCourriel() == null) {
                step.setDestinataireCourriel(modele.getDestinataireCourriel());
                adressees++;
                log.info("Circuit « {} » : le courriel de l'étape « {} » s'adresse désormais à « {} ».",
                        circuit.getNom(), step.getCode(), modele.getDestinataireCourriel());
            }
        }
        return adressees;
    }

    /**
     * Rend au titulaire du dossier les étapes que le circuit livré lui réserve.
     *
     * <p>C'est la seule correction que ce rattrapage s'autorise sur une étape existante, et elle se
     * justifie autrement que par le goût : une étape de traitement ouverte à un <b>rôle</b> laisse
     * tout porteur de ce rôle décider du dossier d'autrui — traiter le plan d'action d'un collègue,
     * et par là ouvrir la clôture d'une non-conformité qui ne le concerne pas. Ce n'est pas une
     * préférence de configuration, c'est une porte laissée ouverte.</p>
     *
     * <p>La désignation du titulaire suit : sans le champ qui la porte, l'étape serait réservée à
     * une personne que rien ne permet de nommer, et deviendrait indécidable.</p>
     */
    private int reserverAuTitulaire(Workflow circuit, Map<String, WorkflowStep> etapesDeReference) {
        int corrigees = 0;

        for (WorkflowStep step : circuit.getSteps()) {
            WorkflowStep modele = etapesDeReference.get(step.getCode());
            if (modele == null) {
                continue;
            }

            if (WorkflowDataInitializer.HABILITATION_TITULAIRE.equals(modele.getResponsableRole())
                    && !WorkflowDataInitializer.HABILITATION_TITULAIRE.equals(step.getResponsableRole())) {
                step.setResponsableRole(WorkflowDataInitializer.HABILITATION_TITULAIRE);
                corrigees++;
                log.info("Circuit « {} » : étape « {} » rendue à son titulaire (elle était ouverte au rôle {}).",
                        circuit.getNom(), step.getCode(), modele.getResponsableRole());
            }

            for (WorkflowTransition transition : step.getTransitions()) {
                WorkflowTransition transitionDeReference = modele.getTransitions().stream()
                        .filter(t -> Objects.equals(t.codeEffectif(), transition.codeEffectif()))
                        .findFirst().orElse(null);
                if (transitionDeReference != null
                        && WorkflowDataInitializer.HABILITATION_TITULAIRE.equals(transitionDeReference.getRequiredRole())
                        && transition.getRequiredRole() == null) {
                    // Sans habilitation propre, la transition rouvre à tous ce que l'étape venait
                    // d'être réservée au titulaire.
                    transition.setRequiredRole(WorkflowDataInitializer.HABILITATION_TITULAIRE);
                    corrigees++;
                }
            }

            if (modele.getChampTitulaire() != null && step.getChampTitulaire() == null) {
                step.setChampTitulaire(modele.getChampTitulaire());
                corrigees++;
                modele.getFields().stream()
                        .filter(champ -> champ.getFieldName().equals(modele.getChampTitulaire()))
                        .filter(champ -> !porteLeChamp(step, champ.getFieldName()))
                        .forEach(champ -> step.getFields().add(WorkflowStepField.builder()
                                .step(step)
                                .fieldName(champ.getFieldName())
                                .fieldLabel(champ.getFieldLabel())
                                .type(champ.getType())
                                .options(champ.getOptions())
                                .decision(champ.getDecision())
                                .isRequired(champ.isRequired())
                                .build()));
                log.info("Circuit « {} » : l'étape « {} » désigne désormais son titulaire par « {} ».",
                        circuit.getNom(), step.getCode(), modele.getChampTitulaire());
            }
        }
        return corrigees;
    }

    private boolean porteLeChamp(WorkflowStep step, String nomDeChamp) {
        return step.getFields().stream()
                .map(WorkflowStepField::getFieldName)
                .anyMatch(nomDeChamp::equals);
    }

    private WorkflowStep destination(WorkflowTransition transition, Map<String, WorkflowStep> presentes) {
        if (transition.getToStep() == null || transition.getToStep().getCode() == null) {
            return null;
        }
        return presentes.get(transition.getToStep().getCode());
    }

    private boolean emetDeja(WorkflowStep step, String codeAction) {
        return step.getTransitions().stream().anyMatch(t -> Objects.equals(t.codeEffectif(), codeAction));
    }

    private WorkflowStep copierEtape(WorkflowStep modele) {
        WorkflowStep copie = WorkflowStep.builder()
                .code(modele.getCode())
                .nomEtape(modele.getNomEtape())
                .stepOrder(modele.getStepOrder())
                .etatTraitement(modele.getEtatTraitement())
                .description(modele.getDescription())
                .responsableRole(modele.getResponsableRole())
                .emailTemplateCode(modele.getEmailTemplateCode())
                .destinataireCourriel(modele.getDestinataireCourriel())
                .champTitulaire(modele.getChampTitulaire())
                .cosignataires(modele.getCosignataires())
                .build();

        modele.getFields().forEach(champ -> copie.getFields().add(WorkflowStepField.builder()
                .step(copie)
                .fieldName(champ.getFieldName())
                .fieldLabel(champ.getFieldLabel())
                .type(champ.getType())
                .options(champ.getOptions())
                .decision(champ.getDecision())
                .actionCode(champ.getActionCode())
                .isRequired(champ.isRequired())
                .build()));
        return copie;
    }

    private WorkflowTransition copierTransition(WorkflowTransition modele, WorkflowStep depuis,
            WorkflowStep vers) {
        return WorkflowTransition.builder()
                .fromStep(depuis)
                .toStep(vers)
                .code(modele.codeEffectif())
                .decision(modele.getDecision())
                .label(modele.getLabel())
                .icon(modele.getIcon())
                .severity(modele.getSeverity())
                .requiredRole(modele.getRequiredRole())
                .conditionRequise(modele.getConditionRequise())
                .conditionLibelle(modele.getConditionLibelle())
                .terminal(modele.isTerminal())
                .build();
    }

    private Map<String, WorkflowStep> parCode(List<WorkflowStep> etapes) {
        Map<String, WorkflowStep> parCode = new LinkedHashMap<>();
        new ArrayList<>(etapes).stream()
                .filter(step -> Objects.nonNull(step.getCode()))
                .forEach(step -> parCode.putIfAbsent(step.getCode(), step));
        return parCode;
    }
}
