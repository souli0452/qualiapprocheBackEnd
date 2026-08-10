package com.qualiapproche.amelioration.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;

import com.qualiapproche.common.utils.StatutEnum;
import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.common.service.SendMailService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.PlanActionService;

import com.qualiapproche.amelioration.utils.ReglagesOrganisation;
import com.qualiapproche.common.utils.ClesReglages;
import lombok.RequiredArgsConstructor;
import com.qualiapproche.amelioration.client.UtilisateurClient;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteResumeMapper;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanActionServiceImpl implements PlanActionService {

    /** Délai d'origine, retenu quand l'organisation n'en a pas fixé d'autre. */
    private static final long SEUIL_DE_RAPPEL_PAR_DEFAUT = 2;

    private final ReglagesOrganisation reglagesOrganisation;
    private final PlanActionMapper planActionMapper;
    private final NonConformiteResumeMapper nonConformiteResumeMapper;
    private final PlanActionRepository planActionRepository;
    private final NonConformiteRepository nonConformiteRepository;
    private final SendMailService sendMailService;
    private final ReferentielClient referentielClient;
    private final PieceJointeStockageService fichierService;
    private final PlansActionDeLaNonConformiteService plansActionService;
    private final WorkflowClient workflowClient;
    private final UtilisateurClient utilisateurClient;

    @Value("${frontend.url}")
    private String frontendUrl;

    /**
     * Supprime un plan d'action qui n'a pas encore été confié.
     *
     * <p>Un plan engagé pilote un circuit et porte l'historique des décisions prises sur lui : le
     * supprimer effacerait cet historique sans que rien ne le remplace, et la non-conformité
     * compterait un plan de moins qu'elle n'en a réellement engagé. Retirer une action décidée est
     * une décision de son circuit, pas une correction de saisie.</p>
     */
    @Override
    public void delete(UUID id) {
        PlanAction planAction = planActionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce plan d'action n'existe pas."));

        if (planAction.getWorkflowId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce plan d'action est engagé auprès de son responsable : il ne peut plus être supprimé.");
        }

        UUID dossier = planAction.getNonConformeId();
        planActionRepository.delete(planAction);

        // Un plan de moins à solder : la condition de clôture peut devenir vraie.
        if (dossier != null) {
            plansActionService.actualiserLesFaits(dossier);
        }
    }

    @Override
    public PlanActionDto createPlanActionDto(PlanActionDto dto) throws IOException {
        PlanAction planAction = planActionMapper.toEntity(dto);

        NonConformite nonConformite = nonConformiteRepository.findById(dto.getNonConformeId())
                .orElseThrow(() -> new RuntimeException("Ce plan d'action n'est associé à aucune non-conformité!"));

        planAction.setNonConformeId(nonConformite.getId());
        // Le numéro du dossier et son processus émetteur étaient laissés à l'écran, qui ne les
        // envoyait pas : les colonnes « N° NC » et « Processus » restaient vides sur toutes les
        // actions, la recherche par numéro n'en trouvait aucune, et les relances d'échéance
        // annonçaient une action rattachée à « null ». Ils sont recopiés depuis le dossier, seul à
        // en faire foi.
        planAction.setNumeroNc(nonConformite.getNumeroReference());
        planAction.setProcEmetteur(nonConformite.getNomProcessus());

        // Le numéro d'ordre n'était renseigné par personne — ni le formulaire, ni le serveur : la
        // colonne « N° Ordre » restait vide sur tous les plans, et l'on ne pouvait désigner une
        // action que par sa description. Il est propre à la non-conformité, comme la numérotation
        // d'un plan d'action l'est en pratique au dossier qui le motive.
        if (planAction.getNumeroOdre() == null || planAction.getNumeroOdre().isBlank()) {
            planAction.setNumeroOdre(prochainNumeroDOrdre(nonConformite.getId()));
        } else {
            planAction.setNumeroOdre(planAction.getNumeroOdre().trim());
        }

        // Un plan qui vient d'être écrit est une <b>proposition</b>, pas un engagement : l'agent
        // imputé le rédige à l'étape de traitement, le pilote le valide, et c'est seulement à la
        // validation qualité qu'il est confié à son responsable et entre dans son circuit
        // (cf. PlansActionDeLaNonConformiteService#confierLesPlans). Ouvrir le circuit ici
        // engageait le responsable sur une action que personne n'avait encore validée.
        if (planAction.getStatus() == null) {
            planAction.setStatus(StatutEnum.INACTIF);
        }

        planAction = planActionRepository.save(planAction);

        // Un plan de plus, non soldé : la clôture de la non-conformité doit se refermer.
        plansActionService.actualiserLesFaits(nonConformite.getId());

        PlanActionDto result = planActionMapper.toDto(planAction);
        result.setFichiers(fichierService.getPjByEntityId(result.getId()));
        rattacherLeDossier(result, nonConformite);
        return result;
    }

    /**
     * Inscrit le responsable que la correction désigne, tant que le plan n'est pas engagé.
     *
     * <p>Un plan engagé a un titulaire côté moteur : c'est lui, et lui seul, à qui l'étape de
     * traitement est ouverte. Le changer ici déplaçait la responsabilité sans que le circuit en
     * sache rien — l'ancien responsable gardait la main sur le dossier, le nouveau ne l'avait pas,
     * et l'historique ne portait aucune trace du transfert. Sur un plan engagé, la ré-attribution
     * est une décision du circuit ; ici, c'est un refus explicite plutôt qu'une divergence
     * silencieuse.</p>
     */
    private void confierAUnAutreResponsable(PlanAction plan, PlanActionDto dto) {
        // Le DTO porte l'identifiant en chaîne, l'entité en UUID : une valeur inexploitable laisse
        // le responsable en place plutôt que de faire échouer la correction entière.
        UUID demande = identifiantOuNull(dto.getResponsableId());
        boolean changement = demande != null && !demande.equals(plan.getResponsableId());

        if (changement && !leResponsableEstChangeable(plan)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le responsable de cette action ne peut plus être changé à ce stade. Il se "
                            + "désigne tant qu'elle est une proposition, ou au moment où le pilote "
                            + "constate sa réalisation.");
        }

        if (demande != null) {
            plan.setResponsableId(demande);
        }
        if (changement) {
            // Le nom et l'adresse sont lus à leur source plutôt que repris de l'écran : celui-ci ne
            // transporte qu'un identifiant, et un libellé recopié se serait périmé au premier
            // changement d'état civil sans que rien ne le rattrape.
            nommerLeResponsable(plan, demande);
        } else if (plan.getResponsableNomComplet() == null && dto.getResponsableNomComplet() != null) {
            plan.setResponsableEmail(dto.getResponsableEmail());
            plan.setResponsableNomComplet(dto.getResponsableNomComplet());
        }
        if (changement && plan.getWorkflowId() != null) {
            transfererLeTitulaire(plan, demande);
        }
    }

    /**
     * Le responsable d'une action est-il encore changeable ?
     *
     * <p>Tant qu'elle est une proposition, oui : rien n'est engagé. Une fois engagée, seulement à
     * l'étape où le pilote constate sa réalisation — c'est là qu'il peut juger qu'elle relève de
     * quelqu'un d'autre. Ailleurs, non : le responsable est en train de la mener, ou elle est déjà
     * jugée, et changer son porteur sans que le circuit en sache rien laisserait l'ancien décider
     * d'une action dont il ne répond plus.</p>
     */
    private boolean leResponsableEstChangeable(PlanAction plan) {
        return plan.getWorkflowId() == null || plan.getStatus() == StatutEnum.EN_VERIFICATION;
    }

    /**
     * Fait suivre au moteur le changement de responsable.
     *
     * <p>Les étapes réservées au titulaire s'ouvrent à la personne que le moteur connaît, non à
     * celle qu'affiche la fiche. Sans cet appel, l'un croirait avoir transféré la responsabilité et
     * l'autre l'ouvrirait toujours à celui qui ne l'a plus.</p>
     */
    private void transfererLeTitulaire(PlanAction plan, UUID nouveau) {
        try {
            workflowClient.designerTitulaire(plan.getId(), nouveau.toString());
            log.info("Action corrective {} : responsabilité transférée à {} et titulaire mis à jour.",
                    plan.getId(), nouveau);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le changement de responsable n'a pas pu être répercuté sur le circuit : "
                            + "il est annulé plutôt que laissé à moitié fait.");
        }
    }

    /**
     * Rang suivant parmi les plans de cette non-conformité.
     *
     * <p>Un simple ordre d'affichage, que la saisie peut reprendre à sa guise : c'est un repère de
     * lecture, non une clé. Il est calculé sur le plus grand rang déjà attribué et non sur le
     * nombre de plans, sans quoi une suppression ferait repartir la numérotation en arrière et le
     * plan ajouté s'insérerait au milieu de la liste.</p>
     */
    private String prochainNumeroDOrdre(UUID nonConformeId) {
        int dernier = planActionRepository.findPlanActionsByNonConformeId(nonConformeId).stream()
                .map(PlanAction::getNumeroOdre)
                .filter(numero -> numero != null && !numero.isBlank())
                .mapToInt(this::rangDuNumero)
                .max()
                .orElse(0);
        return String.valueOf(dernier + 1);
    }

    /** Rang porté par un numéro d'ordre, ou zéro si l'on n'en lit aucun — « P-A-3 » vaut 3. */
    private int rangDuNumero(String numero) {
        String chiffres = numero.replaceAll("\\D", "");
        if (chiffres.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(chiffres);
        } catch (NumberFormatException e) {
            // Un numéro repris d'un autre usage ne doit pas empêcher d'en attribuer un nouveau.
            return 0;
        }
    }

    @Override
    public Page<PlanActionDto> aTraiterParLAppelant(Pageable pageable) {
        List<UUID> aDecider;
        try {
            aDecider = workflowClient.ressourcesADecider("PLAN_ACTION");
        } catch (Exception e) {
            log.warn("Liste « actions à traiter » indisponible, le service de workflow est injoignable : {}",
                    e.getMessage());
            return Page.empty(pageable);
        }

        if (aDecider == null || aDecider.isEmpty()) {
            return Page.empty(pageable);
        }
        return avecLeurDossier(planActionRepository.findByIdIn(aDecider, pageable)
                .map(plan -> {
                    PlanActionDto dto = planActionMapper.toDto(plan);
                    dto.setFichiers(fichierService.getPjByEntityId(plan.getId()));
                    return dto;
                }));
    }

    @Override
    public PlanActionDto corriger(PlanActionDto dto) {
        if (dto == null || dto.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le plan d'action à corriger doit être identifié.");
        }
        PlanAction plan = planActionRepository.findById(dto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce plan d'action n'existe pas."));

        // Seuls les champs de description : le statut, le circuit et le rattachement au dossier
        // relèvent de la décision, pas de la saisie. Recopier le DTO en bloc aurait permis à un
        // écran de solder un plan, ou de le rattacher à une autre non-conformité.
        // Le numéro d'ordre est attribué à la création et n'appartient pas à la saisie : le laisser
        // écrasable permettait à un écran qui ne le renvoie pas de le vider.
        if (dto.getNumeroOdre() != null && !dto.getNumeroOdre().isBlank()) {
            plan.setNumeroOdre(dto.getNumeroOdre().trim());
        }
        // Absent ne veut pas dire vide. Chaque écran ne saisit qu'une partie de l'action — la fiche
        // décrit l'engagement, le dialogue de traitement rapporte le constat — et recopier le DTO
        // champ par champ faisait effacer par le premier ce que le second venait d'écrire : les
        // causes et la solution retenues, saisies au traitement, revenaient à null dès qu'on
        // corrigeait l'action depuis la fiche. Une chaîne vide, elle, efface bel et bien : c'est
        // une saisie, pas une omission.
        // Ce qui est modifiable dépend du moment, non de l'écran qui appelle. Tant que l'action est
        // une **proposition**, on en écrit la description ; une fois **engagée**, on n'en rapporte
        // plus que le constat. Un pilote qui désigne un responsable n'a pas à réécrire l'analyse de
        // l'agent, et un responsable qui rend compte de son traitement n'a pas à redéfinir l'action
        // qu'on lui a confiée.
        if (plan.getWorkflowId() == null) {
            plan.setActionCorrective(valeurRetenue(dto.getActionCorrective(), plan.getActionCorrective()));
            plan.setCritereEfficacite(valeurRetenue(dto.getCritereEfficacite(), plan.getCritereEfficacite()));
            // La cause et la solution retenue appartiennent maintenant à la proposition : c'est la
            // personne imputée qui recherche l'une et arrête l'autre, avant de soumettre le plan.
            // Elles n'étaient modifiables qu'une fois l'action engagée, si bien qu'un plan encore en
            // discussion ne pouvait pas être corrigé sur ce qu'il a de plus substantiel.
            plan.setCauseIdentifiees(valeurRetenue(dto.getCauseIdentifiees(), plan.getCauseIdentifiees()));
            plan.setSolutionRetenues(valeurRetenue(dto.getSolutionRetenues(), plan.getSolutionRetenues()));
            if (dto.getDateEcheance() != null) {
                plan.setDateEcheance(dto.getDateEcheance());
            }
        } else if (plan.getStatus() == StatutEnum.NON_TRAITER) {
            // Le compte rendu appartient à celui qui mène l'action, et au moment où il la mène. Une
            // fois déclarée réalisée, ce qu'il a écrit est ce sur quoi le pilote puis le responsable
            // qualité se prononcent : le laisser réécrire après coup viderait leur avis de son sens.
            plan.setCauseIdentifiees(valeurRetenue(dto.getCauseIdentifiees(), plan.getCauseIdentifiees()));
            plan.setSolutionRetenues(valeurRetenue(dto.getSolutionRetenues(), plan.getSolutionRetenues()));
            plan.setObservation(valeurRetenue(dto.getObservation(), plan.getObservation()));
        }
        confierAUnAutreResponsable(plan, dto);
        plan.setNumeroTelephone(valeurRetenue(dto.getNumeroTelephone(), plan.getNumeroTelephone()));

        PlanActionDto corrige = planActionMapper.toDto(planActionRepository.save(plan));
        // Désigner un responsable est ce que le pilote fait à la validation : le fait qui ouvre sa
        // transition en dépend, et rien d'autre ne le lui dirait.
        plansActionService.actualiserLesFaits(plan.getNonConformeId());
        corrige.setFichiers(fichierService.getPjByEntityId(plan.getId()));
        return avecSonDossier(corrige);
    }

    /**
     * Valeur à retenir pour un champ de saisie : celle qui est proposée, ou celle en place si rien
     * n'est proposé. {@code null} signifie « non saisi » ; une chaîne vide signifie « effacé ».
     */
    private String valeurRetenue(String propose, String enPlace) {
        return propose == null ? enPlace : propose;
    }

    private UUID identifiantOuNull(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valeur.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Identifiant de responsable inexploitable : {}", valeur);
            return null;
        }
    }

    /**
     * Joint à une action le dossier qui l'a motivée.
     *
     * <p>Une action corrective ne se comprend pas seule : on ne peut ni la mener ni juger de son
     * effet sans savoir ce qui a été constaté, sur quel processus, et par qui. L'action ne portait
     * que l'identifiant de son dossier — une valeur qu'aucun écran ne peut afficher — si bien que le
     * responsable devait rouvrir la non-conformité dans une autre liste pour lire ce qu'on lui
     * demandait de corriger.</p>
     */
    private PlanActionDto avecSonDossier(PlanActionDto dto) {
        if (dto == null || dto.getNonConformeId() == null) {
            return dto;
        }
        nonConformiteRepository.findById(dto.getNonConformeId())
                .ifPresent(dossier -> rattacherLeDossier(dto, dossier));
        return dto;
    }

    /**
     * Le même rattachement, pour une page entière : les dossiers sont lus en une fois.
     *
     * <p>Une lecture par ligne aurait coûté autant d'allers-retours que la page a d'actions, et
     * plusieurs y renvoient au même dossier.</p>
     */
    private Page<PlanActionDto> avecLeurDossier(Page<PlanActionDto> page) {
        List<UUID> dossiers = page.getContent().stream()
                .map(PlanActionDto::getNonConformeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (dossiers.isEmpty()) {
            return page;
        }

        Map<UUID, NonConformite> parIdentifiant = new HashMap<>();
        nonConformiteRepository.findAllById(dossiers)
                .forEach(dossier -> parIdentifiant.put(dossier.getId(), dossier));

        page.getContent().forEach(dto -> {
            NonConformite dossier = parIdentifiant.get(dto.getNonConformeId());
            if (dossier != null) {
                rattacherLeDossier(dto, dossier);
            }
        });
        return page;
    }

    /**
     * Le numéro et le processus restent relus du dossier quand l'action ne les porte pas : les
     * actions créées avant que la création ne les recopie ont ces colonnes vides, et il n'y a pas de
     * raison de les afficher amputées en attendant une reprise de données.
     */
    private void rattacherLeDossier(PlanActionDto dto, NonConformite dossier) {
        dto.setNonConformite(nonConformiteResumeMapper.versResume(dossier));
        if (dto.getNumeroNc() == null || dto.getNumeroNc().isBlank()) {
            dto.setNumeroNc(dossier.getNumeroReference());
        }
        if (dto.getProcEmetteur() == null || dto.getProcEmetteur().isBlank()) {
            dto.setProcEmetteur(dossier.getNomProcessus());
        }
    }

    @Override
    public Page<PlanActionDto> allPlanActions(Pageable pageable) {
        return avecLeurDossier(planActionRepository.findAll(pageable)
                .map(planActionMapper::toDto)
                .map(dto -> {
                    dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
                    return dto;
                }));
    }

    @Override
    public Page<PlanActionDto> planActionByResponsable(String responsable, StatutEnum statut, Pageable pageable) {
        return avecLeurDossier(planActionRepository.findPlanActionsByResponsableEmailAndStatus(responsable, statut, pageable)
                .map(planActionMapper::toDto)
                .map(dto -> {
                    dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
                    return dto;
                }));
    }

    @Override
    public PlanActionDto getPlanActionDtoById(UUID id) {
        PlanAction planAction = planActionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce plan d'action n'existe pas."));
        PlanActionDto dto = planActionMapper.toDto(planAction);
        dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
        return avecSonDossier(dto);
    }

    @Override
    public Page<PlanActionDto> planActionByResponsableAll(String responsable, Pageable pageable) {
        return avecLeurDossier(planActionRepository.findPlanActionsByResponsableEmail(responsable, pageable)
                .map(planActionMapper::toDto)
                .map(dto -> {
                    dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
                    return dto;
                }));
    }

    @Override
    public Map<String, Map<String, Map<String, Long>>> getFrequenceTraitementParMois(int annee) {
        // 1. Définir la plage temporelle
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Récupérer les données
        List<Object[]> resultats = planActionRepository.countStatusByMonth(debutAnnee, finAnnee);

        // 3. Initialiser la structure de réponse
        Map<String, Map<String, Long>> stats = new LinkedHashMap<>();
        MOIS.forEach(mois -> stats.put(mois, new HashMap<>()));

        // 4. Peupler les résultats
        resultats.forEach(row -> {
            int moisIndex = ((Number) row[0]).intValue() - 1;
            String statut = ((String) row[1]);
            long count = ((Number) row[2]).longValue();

            if (moisIndex >= 0 && moisIndex < MOIS.size()) {
                String mois = MOIS.get(moisIndex);
                stats.get(mois).put(statut, count);
            }
        });

        // 5. Calculer les fréquences
        Map<String, Map<String, Long>> frequences = new LinkedHashMap<>();
        stats.forEach((mois, statuts) -> {
            long total = statuts.values().stream().mapToLong(Long::longValue).sum();
            long traites = statuts.getOrDefault("TRAITER", 0L);

            Map<String, Long> freqMois = new HashMap<>();
            freqMois.put("taux_traitement", total > 0 ? (traites * 100) / total : 0L);
            freqMois.put("total", total);

            frequences.put(mois, freqMois);
        });

        return Collections.singletonMap(String.valueOf(annee), frequences);
    }

    /**
     * Tournée de nuit des relances : prévient les responsables dont l'échéance approche.
     *
     * <p>Le seuil vient des réglages de l'organisation ({@code RAPPEL_ECHEANCE_JOURS}). Il était
     * jusqu'ici figé à deux jours dans le code, alors même que l'écran de configuration proposait de
     * le saisir : la valeur saisie n'avait aucun effet. Réglage vide ou référentiel injoignable, les
     * deux jours d'origine s'appliquent.</p>
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void rappelEcheance() {
        List<PlanAction> planActions = planActionRepository.findPlanActionsByStatus(StatutEnum.NON_TRAITER);
        LocalDate today = LocalDate.now();
        long seuilDeRappel = reglagesOrganisation.entier(ClesReglages.RAPPEL_ECHEANCE_JOURS,
                SEUIL_DE_RAPPEL_PAR_DEFAUT);

        for (PlanAction action : planActions) {
            String subject = "Traitement du plan d'action N° ordre " + action.getNumeroOdre();
            String link = frontendUrl + "/traitement-action/non-traiter";

            LocalDate echeance = action.getDateEcheance();
            long joursRestants = ChronoUnit.DAYS.between(today, echeance);

            if (joursRestants > seuilDeRappel) {
                // Trop tôt pour relancer : le responsable a encore le temps.
                continue;
            }

            if (joursRestants >= 1) {
                sendMailService.sendMailToUserAfterDemandImputed(action.getResponsableEmail(), subject, link,
                        "alertePlanAction", action.getResponsableNomComplet(), action.getNumeroNc(),
                        String.valueOf(joursRestants));
            } else if (joursRestants == 0) {
                sendMailService.sendMailToUserAfterDemandImputed(action.getResponsableEmail(), subject, link,
                        "alerteLastDay", action.getResponsableNomComplet(), action.getNumeroNc(),
                        String.valueOf(joursRestants));
            } else {

                sendMailService.sendMailToUserAfterDemandImputed(action.getResponsableEmail(), subject, link,
                        "alerteEpuise", action.getResponsableNomComplet(), action.getNumeroNc(),
                        String.valueOf(joursRestants));
            }
        }
    }

    private static final List<String> MOIS = List.of(
            "janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre");

    /*
     * public void deletePlanAction(UUID id) {
     * PlanAction planAction = planActionRepository.getReferenceById(id);
     * if (planAction != null) {
     * // Retirer la planAction de la collection
     * NonConformite nonConformite = planAction.getNonConformite();
     * nonConformite.getPlanActions().remove(planAction); // Retirer de la liste
     * // Supprimer la planAction
     * planActionRepository.delete(planAction);
     * }
     * }
     */

    @Override
    public Page<PlanActionDto> getPlanActionsByStructure(String structureId, Pageable pageable) {
        return avecLeurDossier(planActionRepository.findPlanActionsByStructureId(structureId, pageable)
                .map(planActionMapper::toDto)
                .map(dto -> {
                    dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
                    return dto;
                }));
    }

    @Override
    public Page<PlanActionDto> search(
            String numeroOdre, String responsableEmail, String responsableNomComplet,
            String numeroNc, StatutEnum status, UUID nonConformeId,
            LocalDate dateEcheanceFrom, LocalDate dateEcheanceTo,
            LocalDate dateTraitementFrom, LocalDate dateTraitementTo,
            Pageable pageable) {
        // Not implemented in repository yet, returning empty for now or using a spec
        // Assuming a method doesn't exist yet, we will just return a basic findAll to satisfy the interface temporarily
        return planActionRepository.findAll(pageable)
                .map(planActionMapper::toDto);
    }

    /**
     * Consigne sur l'action ce que le responsable qualité a observé.
     *
     * <p>Le critère d'efficacité était recueilli à la définition de l'action et n'était confronté à
     * rien : on savait ce qu'on attendait, jamais ce qu'on avait obtenu. Sans cette trace, l'étape
     * de mesure ne serait qu'un passage de plus.</p>
     */
    private void appliquerLeConstatDEfficacite(PlanAction planAction, Map<String, String> champs) {
        if (champs == null) {
            return;
        }
        String constat = champs.get("constatEfficacite");
        if (constat != null && !constat.isBlank()) {
            planAction.setConstatEfficacite(constat.trim());
        }
    }

    /**
     * Consigne sur l'action ce que son responsable a rapporté en la déclarant réalisée.
     *
     * <p>Le compte rendu — la cause identifiée, la solution mise en œuvre — était saisi sur l'écran
     * de traitement puis enregistré par un appel distinct de la décision. Les deux partaient
     * ensemble, dans un ordre que rien ne garantissait : la décision passée la première faisait
     * quitter au plan l'étape de réalisation, seule où {@link #corriger} accepte encore le compte
     * rendu, et ce que le responsable avait écrit était refusé en silence. Le pilote constatait
     * alors une action dont il ne lisait ni cause ni solution.</p>
     *
     * <p>Porté par l'étape, il arrive par la décision elle-même : un seul chemin, une seule
     * transaction, et l'historique du moteur en garde la trace. Une valeur vide n'efface pas ce qui
     * est en place — la plupart des franchissements ne rapportent rien.</p>
     */
    private void appliquerLeCompteRendu(PlanAction planAction, Map<String, String> champs) {
        if (champs == null) {
            return;
        }
        String causes = champs.get("causeIdentifiees");
        if (causes != null && !causes.isBlank()) {
            planAction.setCauseIdentifiees(causes.trim());
        }
        String solutions = champs.get("solutionRetenues");
        if (solutions != null && !solutions.isBlank()) {
            planAction.setSolutionRetenues(solutions.trim());
        }
    }

    /**
     * Inscrit sur le plan le responsable que l'étape vient de désigner.
     *
     * <p>Une ré-attribution qui ne changerait que le titulaire côté moteur laisserait le plan porter
     * l'ancien nom : les deux doivent dire la même chose, sans quoi l'écran et le circuit
     * désigneraient deux personnes différentes.</p>
     */
    private void appliquerLeResponsableSaisi(PlanAction planAction, Map<String, String> champs) {
        if (champs == null) {
            return;
        }
        String saisi = champs.get("responsableId");
        if (saisi == null || saisi.isBlank()) {
            return;
        }
        UUID nouveau;
        try {
            nouveau = UUID.fromString(saisi.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Responsable ré-attribué illisible pour le plan {} : {}", planAction.getId(), saisi);
            return;
        }
        if (nouveau.equals(planAction.getResponsableId())) {
            return;
        }

        log.info("Plan d'action {} : responsabilité transférée de {} à {} par décision du circuit.",
                planAction.getId(), planAction.getResponsableId(), nouveau);
        planAction.setResponsableId(nouveau);
        nommerLeResponsable(planAction, nouveau);
    }

    /**
     * Inscrit sur le plan le nom et l'adresse du responsable que le circuit vient de désigner.
     *
     * <p>Le circuit ne transporte qu'un identifiant. Ne recopier que lui laissait la fiche afficher
     * le <b>nouvel</b> identifiant sous l'<b>ancien</b> nom : plus rien n'y disait qui répondait de
     * l'action, et les relances partaient à la mauvaise adresse.</p>
     *
     * <p>Une indisponibilité de l'annuaire n'annule pas le transfert — la responsabilité a changé,
     * c'est le fait principal — mais efface le nom devenu faux plutôt que de le laisser mentir.</p>
     */
    private void nommerLeResponsable(PlanAction planAction, UUID responsableId) {
        try {
            Map<String, Object> reponse = utilisateurClient.getUserById(responsableId.toString());
            Object donnees = reponse == null ? null : reponse.getOrDefault("data", reponse);
            if (donnees instanceof Map<?, ?> utilisateur) {
                planAction.setResponsableEmail(texte(utilisateur.get("email")));
                planAction.setResponsableNomComplet(texte(utilisateur.get("nomComplet")));
                return;
            }
            log.warn("Réponse inattendue de user-service pour l'utilisateur {}.", responsableId);
        } catch (Exception e) {
            log.warn("Le nom du nouveau responsable du plan {} n'a pas pu être lu : {}",
                    planAction.getId(), e.getMessage());
        }
        planAction.setResponsableEmail(null);
        planAction.setResponsableNomComplet(null);
    }

    private String texte(Object valeur) {
        if (valeur == null) {
            return null;
        }
        String chaine = valeur.toString().trim();
        return chaine.isEmpty() ? null : chaine;
    }

    @Override
    public void updateWorkflowState(UUID planActionId, String newStateName, String newEtatTraitement,
            Map<String, String> champs) {
        PlanAction planAction = planActionRepository.findById(planActionId)
                .orElseThrow(() -> new RuntimeException("Plan d'action introuvable: " + planActionId));

        planAction.setWorkflowStatus(newStateName);

        // etatCode est absent des notifications d'état terminal : sans ce garde,
        // StatutEnum.valueOf(null) lèverait une NullPointerException, non couverte par le catch.
        if (newEtatTraitement != null && !newEtatTraitement.isBlank()) {
            try {
                StatutEnum statut = StatutEnum.valueOf(newEtatTraitement);
                planAction.setStatus(statut);
                // La date du traitement est celle où l'action a été menée, non celle où elle a été
                // reconnue efficace : entre les deux, il peut s'écouler le temps qu'il faut pour en
                // mesurer l'effet, et dater l'action de ce jour-là en fausserait tout suivi.
                boolean realisee = statut == StatutEnum.EN_VERIFICATION || statut == StatutEnum.TRAITER;
                if (realisee && planAction.getDateTraitement() == null) {
                    planAction.setDateTraitement(LocalDate.now());
                }
            } catch (IllegalArgumentException e) {
                log.warn("EtatTraitement non reconnu dans le workflow: {}", newEtatTraitement);
            }
        }

        appliquerLeResponsableSaisi(planAction, champs);
        appliquerLeCompteRendu(planAction, champs);
        appliquerLeConstatDEfficacite(planAction, champs);

        planActionRepository.save(planAction);

        // Le solde des plans est déclaré au circuit de la non-conformité, il ne la fait pas
        // avancer. Le dernier plan traité franchissait auparavant une étape au nom de « SYSTEM » :
        // le dossier progressait sans que personne ne l'ait décidé, et l'historique portait une
        // décision qu'aucun responsable qualité n'avait prise. La condition ouvre la clôture ; la
        // clôture reste une décision.
        plansActionService.actualiserLeFaitDeSolde(planAction.getNonConformeId());
    }
}
