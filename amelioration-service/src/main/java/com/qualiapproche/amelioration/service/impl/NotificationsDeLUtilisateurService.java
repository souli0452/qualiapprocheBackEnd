package com.qualiapproche.amelioration.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.common.dto.NotificationDto;
import com.qualiapproche.common.dto.WorkflowStateDto;
import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.common.enumeration.SourceNotification;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.utils.ClesReglages;
import com.qualiapproche.common.utils.StatutEnum;
import com.qualiapproche.amelioration.utils.ReglagesOrganisation;
import com.qualiapproche.common.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ce que l'utilisateur connecté a en attente dans le module amélioration.
 *
 * <p>La liste est <b>calculée à chaque demande</b>, et rien n'en est conservé. C'est ce qui la rend
 * juste : une notification n'existe pas en propre, elle n'est que la lecture d'un travail qui
 * attend. Un dossier traité entre deux consultations disparaît de lui-même, et aucun geste n'a à
 * marquer quoi que ce soit comme lu — un registre persisté aurait, lui, réclamé une écriture à
 * chaque transition, une purge, et se serait mis à mentir au premier événement manqué.</p>
 *
 * <p>La cloche assemblait ces lignes elle-même, en accumulant ce que chaque écran lui rapportait au
 * fil des visites. Elle n'était donc juste que pour qui avait ouvert toutes les pages, et
 * l'utilisateur qui allait droit à son tableau de bord n'y voyait rien.</p>
 *
 * <p>Qui a le droit de décider quoi n'est pas rejugé ici : la question est posée au moteur, qui
 * applique l'habilitation de l'étape courante. La rejouer aurait constitué une seconde table de
 * règles, et les deux auraient divergé.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationsDeLUtilisateurService {

    /** Repères stables sur lesquels l'écran branche sa destination et son icône. */
    public static final String CODE_BROUILLON = "NC_BROUILLON";
    public static final String CODE_NC_A_DECIDER = "NC_A_DECIDER";
    public static final String CODE_PLAN_A_DECIDER = "PLAN_ACTION_A_DECIDER";
    public static final String CODE_ECHEANCE_DEPASSEE = "PLAN_ACTION_ECHEANCE_DEPASSEE";
    public static final String CODE_ECHEANCE_PROCHE = "PLAN_ACTION_ECHEANCE_PROCHE";

    /** Nombre de jours avant l'échéance à partir duquel le plan est annoncé, à défaut de réglage. */
    private static final long SEUIL_DE_RAPPEL_PAR_DEFAUT = 2;

    private final NonConformiteRepository nonConformiteRepository;
    private final PlanActionRepository planActionRepository;
    private final WorkflowClient workflowClient;
    private final ReglagesOrganisation reglagesOrganisation;

    /**
     * Les lignes destinées à l'appelant, les décisions ouvertes d'abord.
     *
     * <p>Une source indisponible retire ses lignes sans faire échouer les autres : mieux vaut une
     * cloche incomplète, qui se remplira au rétablissement, qu'un écran en erreur.</p>
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> pourLAppelant() {
        String utilisateur = SecurityUtils.getCurrentUserId();
        List<NotificationDto> lignes = new ArrayList<>();

        lignes.addAll(nonConformitesADecider());
        lignes.addAll(plansActionADecider());
        lignes.addAll(echeancesDeMesPlans());
        brouillonsAFinaliser(utilisateur).ifPresent(lignes::add);

        return lignes;
    }

    /**
     * Les non-conformités dont le circuit ouvre une décision à l'appelant, ventilées par étape.
     *
     * <p>C'est le moteur qui nomme l'étape, et non une table de correspondance tenue ici : les
     * intitulés du circuit sont paramétrables, et toute liste figée côté module aurait cessé d'être
     * exacte à la première étape ajoutée.</p>
     */
    private List<NotificationDto> nonConformitesADecider() {
        List<UUID> aDecider = ressources("NON_CONFORMITE");
        if (aDecider.isEmpty()) {
            return List.of();
        }

        Map<UUID, WorkflowStateDto> etats;
        try {
            etats = workflowClient.getWorkflowStates(aDecider);
        } catch (Exception e) {
            log.warn("Étapes des dossiers à décider indisponibles : {}", e.getMessage());
            etats = Map.of();
        }

        // L'ordre de parcours est conservé : les étapes se présentent comme le moteur les a rendues.
        Map<String, Long> parEtape = new LinkedHashMap<>();
        for (UUID id : aDecider) {
            WorkflowStateDto etat = etats.get(id);
            String etape = etat != null ? etat.getCurrentStateName() : null;
            parEtape.merge(renseigne(etape) ? etape : "Sans étape nommée", 1L, Long::sum);
        }

        List<NotificationDto> lignes = new ArrayList<>();
        parEtape.forEach((etape, nombre) -> lignes.add(NotificationDto.builder()
                .source(SourceNotification.AMELIORATION)
                .code(CODE_NC_A_DECIDER)
                .titre(etape)
                .detail(nombre == 1
                        ? "Une non-conformité attend votre décision à cette étape."
                        : nombre + " non-conformités attendent votre décision à cette étape.")
                .gravite(GraviteNotification.ATTENTION)
                .nombre(nombre)
                .build()));
        return lignes;
    }

    /** Les plans d'action dont le circuit ouvre une décision à l'appelant. */
    private List<NotificationDto> plansActionADecider() {
        long nombre = ressources("PLAN_ACTION").size();
        if (nombre == 0) {
            return List.of();
        }
        return List.of(NotificationDto.builder()
                .source(SourceNotification.AMELIORATION)
                .code(CODE_PLAN_A_DECIDER)
                .titre("Plans d'action à traiter")
                .detail(nombre == 1
                        ? "Un plan d'action attend votre décision."
                        : nombre + " plans d'action attendent votre décision.")
                .gravite(GraviteNotification.ATTENTION)
                .nombre(nombre)
                .build());
    }

    /**
     * Les non-conformités que l'appelant a commencées sans les soumettre.
     *
     * <p>Rien ne les réclame et personne ne les attend : elles informent, là où une décision
     * ouverte retient le dossier.</p>
     */
    private Optional<NotificationDto> brouillonsAFinaliser(String utilisateur) {
        if (!renseigne(utilisateur)) {
            return Optional.empty();
        }
        long nombre = nonConformiteRepository.countByCreatedByIdAndStatus(utilisateur, Status.DRAFT);
        if (nombre == 0) {
            return Optional.empty();
        }
        return Optional.of(NotificationDto.builder()
                .source(SourceNotification.AMELIORATION)
                .code(CODE_BROUILLON)
                .titre("Brouillons à finaliser")
                .detail(nombre == 1
                        ? "Une non-conformité reste à finaliser."
                        : nombre + " non-conformités restent à finaliser.")
                .gravite(GraviteNotification.INFO)
                .nombre(nombre)
                .build());
    }

    /**
     * Les plans d'action dont l'appelant est responsable et dont l'échéance presse.
     *
     * <p>Une relance quotidienne le prévient déjà par courriel. Elle ne part qu'une fois, le jour
     * où le seuil est franchi : qui l'a manquée, ou qui l'a lue un lundi matin parmi trente autres,
     * n'a plus rien qui le lui rappelle. La cloche, elle, le redit tant que le plan n'est pas
     * traité.</p>
     *
     * <p>Le seuil est celui de l'organisation ({@code RAPPEL_ECHEANCE_JOURS}), le même que la
     * relance : deux avertissements qui ne se déclencheraient pas au même moment se
     * contrediraient.</p>
     *
     * <p>Deux lignes distinctes, et non une seule : un plan en retard et un plan qui arrive à terme
     * n'appellent pas le même geste, et les fondre aurait noyé le retard dans l'échéance proche.</p>
     */
    private List<NotificationDto> echeancesDeMesPlans() {
        String courriel = SecurityUtils.getCurrentUserEmail();
        if (!renseigne(courriel)) {
            return List.of();
        }

        List<PlanAction> miens = planActionRepository
                .findPlanActionsByResponsableEmailAndStatus(courriel, StatutEnum.NON_TRAITER);
        if (miens == null || miens.isEmpty()) {
            return List.of();
        }

        long seuil = reglagesOrganisation.entier(ClesReglages.RAPPEL_ECHEANCE_JOURS, SEUIL_DE_RAPPEL_PAR_DEFAUT);
        LocalDate aujourdHui = LocalDate.now();
        long depassees = 0;
        long proches = 0;

        for (PlanAction plan : miens) {
            LocalDate echeance = plan.getDateEcheance();
            if (echeance == null) {
                // Un plan sans échéance n'est ni en retard ni à échéance proche : le compter d'un
                // côté ou de l'autre inventerait une date que personne n'a saisie.
                continue;
            }
            long jours = ChronoUnit.DAYS.between(aujourdHui, echeance);
            if (jours < 0) {
                depassees++;
            } else if (jours <= seuil) {
                proches++;
            }
        }

        List<NotificationDto> lignes = new ArrayList<>();
        if (depassees > 0) {
            lignes.add(NotificationDto.builder()
                    .source(SourceNotification.AMELIORATION)
                    .code(CODE_ECHEANCE_DEPASSEE)
                    .titre("Plans d'action en retard")
                    .detail(depassees == 1
                            ? "Un plan d'action dont vous répondez a dépassé son échéance."
                            : depassees + " plans d'action dont vous répondez ont dépassé leur échéance.")
                    .gravite(GraviteNotification.URGENT)
                    .nombre(depassees)
                    .build());
        }
        if (proches > 0) {
            lignes.add(NotificationDto.builder()
                    .source(SourceNotification.AMELIORATION)
                    .code(CODE_ECHEANCE_PROCHE)
                    .titre("Échéances proches")
                    .detail(proches == 1
                            ? "Un plan d'action dont vous répondez arrive à échéance."
                            : proches + " plans d'action dont vous répondez arrivent à échéance.")
                    .gravite(GraviteNotification.ATTENTION)
                    .nombre(proches)
                    .build());
        }
        return lignes;
    }

    /** Interroge le moteur, et rend une liste vide plutôt qu'une erreur s'il est hors d'atteinte. */
    private List<UUID> ressources(String typeDeRessource) {
        try {
            List<UUID> ids = workflowClient.ressourcesADecider(typeDeRessource);
            return ids == null ? List.of() : ids;
        } catch (Exception e) {
            log.warn("Dossiers « à décider » ({}) indisponibles, le moteur est injoignable : {}",
                    typeDeRessource, e.getMessage());
            return List.of();
        }
    }

    private static boolean renseigne(String valeur) {
        return valeur != null && !valeur.isBlank();
    }
}
