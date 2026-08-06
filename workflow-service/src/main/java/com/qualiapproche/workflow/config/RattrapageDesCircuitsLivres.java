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

    private final WorkflowRepository workflowRepository;

    @Override
    @Transactional
    public void run(String... args) {
        completer("NON_CONFORMITE", WorkflowDataInitializer::circuitNonConformiteParDefaut);
        completer("PLAN_ACTION", WorkflowDataInitializer::circuitPlanActionParDefaut);
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

            int ajouts = ajouterLesEtapesManquantes(circuit, reference, etapesDeReference)
                    + ajouterLesTransitionsManquantes(circuit, etapesDeReference)
                    + ajouterLesChampsManquants(circuit, etapesDeReference)
                    + retirerLesChampsAbandonnes(circuit)
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
                .champTitulaire(modele.getChampTitulaire())
                .build();

        modele.getFields().forEach(champ -> copie.getFields().add(WorkflowStepField.builder()
                .step(copie)
                .fieldName(champ.getFieldName())
                .fieldLabel(champ.getFieldLabel())
                .type(champ.getType())
                .options(champ.getOptions())
                .decision(champ.getDecision())
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
