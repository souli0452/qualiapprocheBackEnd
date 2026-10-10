package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.dto.NotificationDto;
import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.common.enumeration.SourceNotification;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.evaluation.dto.FiltreProgramme;
import com.qualiapproche.evaluation.dto.NotificationsAuditResumeDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.enumeration.ConstatRQAPBF;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutChecklist;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.AuditeurRepository;
import com.qualiapproche.evaluation.repository.ChecklistAuditRepository;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.NotationCritereRepository;
import com.qualiapproche.evaluation.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ce que le module Audit attend de l'utilisateur connecté : les lignes de la cloche et les
 * compteurs des onglets, sur le modèle du module Non-conformités.
 *
 * <p>Tout est recalculé à chaque appel et jamais conservé : une ligne disparaît d'elle-même une
 * fois le travail fait. Les files sont celles des écrans : audits à valider ou en retard
 * (Programme), points à saisir, brouillons à publier et constats compilés à valider
 * (Constatations), écarts RQAP-BF dont la transmission attend (Suivi), modèles de checklist en
 * brouillon (Checklists), et les signatures de rapport qui attendent l'appelant.</p>
 *
 * <p>Un auditeur ne voit que ce qui relève de ses audits ; un appelant qui n'est pas au vivier
 * (responsable qualité, direction) voit ce qui attend une décision dans toute la direction.</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationsAuditService {

    public static final String CODE_A_VALIDER = "AUDIT_A_VALIDER";
    public static final String CODE_EN_RETARD = "AUDIT_EN_RETARD";
    public static final String CODE_DEMARRE_BIENTOT = "AUDIT_DEMARRE_BIENTOT";
    public static final String CODE_POINTS_A_SAISIR = "CONSTATS_A_SAISIR";
    public static final String CODE_BROUILLONS = "CONSTATS_BROUILLON";
    public static final String CODE_COMPILES_A_VALIDER = "CONSTATS_A_VALIDER";
    public static final String CODE_SIGNATURE = "SIGNATURE_ATTENDUE";
    public static final String CODE_ECARTS = "ECARTS_A_TRANSMETTRE";
    public static final String CODE_CHECKLISTS = "CHECKLIST_BROUILLON";

    /** Un audit qui démarre d'ici là est annoncé. */
    private static final long JOURS_AVANT_DEMARRAGE = 7;

    private final AuditRepository audits;
    private final AuditeurRepository auditeurs;
    private final ConstatRepository constats;
    private final SignatureRepository signatures;
    private final ChecklistAuditRepository checklists;
    private final NotationCritereRepository notations;

    @Transactional(readOnly = true)
    public List<NotificationDto> pourLAppelant() {
        Etat e = etat();
        List<NotificationDto> lignes = new ArrayList<>();
        ligne(lignes, CODE_EN_RETARD, GraviteNotification.URGENT, e.enRetard, "Audits en retard",
                "audit validé n'a pas démarré à la date prévue", "audits validés n'ont pas démarré à la date prévue");
        ligne(lignes, CODE_SIGNATURE, GraviteNotification.URGENT, e.signatures, "Rapports à signer",
                "rapport d'audit attend votre signature", "rapports d'audit attendent votre signature");
        ligne(lignes, CODE_A_VALIDER, GraviteNotification.ATTENTION, e.aValider, "Audits à valider",
                "audit planifié attend sa validation", "audits planifiés attendent leur validation");
        ligne(lignes, CODE_COMPILES_A_VALIDER, GraviteNotification.ATTENTION, e.compiles, "Constats à valider",
                "constat compilé attend la revue d'équipe", "constats compilés attendent la revue d'équipe");
        ligne(lignes, CODE_POINTS_A_SAISIR, GraviteNotification.INFO, e.pointsASaisir, "Points de contrôle à saisir",
                "point de contrôle vous est assigné sans constat", "points de contrôle vous sont assignés sans constat");
        ligne(lignes, CODE_BROUILLONS, GraviteNotification.INFO, e.brouillons, "Constats à publier",
                "constat en brouillon n'a pas rejoint la compilation", "constats en brouillon n'ont pas rejoint la compilation");
        ligne(lignes, CODE_DEMARRE_BIENTOT, GraviteNotification.INFO, e.demarrentBientot, "Audits qui démarrent",
                "audit démarre dans les " + JOURS_AVANT_DEMARRAGE + " jours", "audits démarrent dans les " + JOURS_AVANT_DEMARRAGE + " jours");
        ligne(lignes, CODE_ECARTS, GraviteNotification.INFO, e.ecarts, "Écarts RQAP-BF à transmettre",
                "écart publié attend sa transmission au module NC", "écarts publiés attendent leur transmission au module NC");
        ligne(lignes, CODE_CHECKLISTS, GraviteNotification.INFO, e.checklistsBrouillon, "Checklists à publier",
                "modèle de checklist est encore en brouillon", "modèles de checklist sont encore en brouillon");
        return lignes;
    }

    @Transactional(readOnly = true)
    public NotificationsAuditResumeDto resume() {
        Etat e = etat();
        long programme = e.aValider + e.enRetard;
        long constatsAttendus = e.pointsASaisir + e.brouillons + e.compiles;
        return NotificationsAuditResumeDto.builder()
                .programme(programme)
                .constats(constatsAttendus)
                .suivi(e.ecarts)
                .checklists(e.checklistsBrouillon)
                .signatures(e.signatures)
                .total(programme + constatsAttendus + e.ecarts + e.checklistsBrouillon + e.signatures)
                .build();
    }

    private void ligne(List<NotificationDto> lignes, String code, GraviteNotification gravite, long nombre,
                       String titre, String singulier, String pluriel) {
        if (nombre <= 0) {
            return;
        }
        lignes.add(NotificationDto.builder()
                .source(SourceNotification.EVALUATION)
                .code(code)
                .titre(titre)
                .detail(nombre + " " + (nombre == 1 ? singulier : pluriel) + ".")
                .gravite(gravite)
                .nombre(nombre)
                .build());
    }

    /** Les nombres, calculés une fois pour la cloche comme pour les compteurs. */
    private Etat etat() {
        UUID directionId = Perimetre.exigerUneDirection();
        String moi = SecurityUtils.getCurrentUserId();
        LocalDate aujourdHui = LocalDate.now();
        Etat e = new Etat();

        List<Audit> ouverts = audits.findAll(SpecificationsAudit.du(directionId, FiltreProgramme.builder().build(),
                        null, aujourdHui)).stream()
                .filter(a -> !a.getStatut().estTermine())
                .toList();
        e.aValider = ouverts.stream().filter(a -> a.getStatut() == StatutAudit.PLANIFIE).count();
        e.enRetard = ouverts.stream().filter(a -> a.estEnRetard(aujourdHui)).count();
        e.demarrentBientot = ouverts.stream()
                .filter(a -> a.getStatut() == StatutAudit.EN_PREPARATION && a.getDateDebutPrevue() != null)
                .filter(a -> !a.getDateDebutPrevue().isBefore(aujourdHui)
                        && !a.getDateDebutPrevue().isAfter(aujourdHui.plusDays(JOURS_AVANT_DEMARRAGE)))
                .count();

        List<Audit> enCours = ouverts.stream().filter(a -> a.getStatut() == StatutAudit.EN_COURS).toList();
        Optional<Auditeur> auditeur = auditeurs.findByDirectionIdAndUtilisateurId(directionId, moi);
        if (auditeur.isPresent()) {
            Auditeur a = auditeur.get();
            for (Audit audit : enCours.stream().filter(x -> x.aDansLEquipe(a)).toList()) {
                Set<UUID> pointsSaisis = constats.findAllByAuditIdAndRedacteurId(audit.getId(), a.getId()).stream()
                        .map(Constat::getPointControle).filter(p -> p != null).map(p -> p.getId())
                        .collect(Collectors.toSet());
                e.pointsASaisir += audit.getChecklists().stream().flatMap(c -> c.getPoints().stream())
                        .filter(p -> p.getAuditeurAssigne() != null && p.getAuditeurAssigne().getId().equals(a.getId()))
                        .filter(p -> !pointsSaisis.contains(p.getId()))
                        .count();
                e.brouillons += constats.findAllByAuditIdAndRedacteurIdAndStatutOrderByDateSaisieAsc(audit.getId(),
                        a.getId(), StatutConstat.BROUILLON).size();
                // La revue d'équipe est menée par le responsable : c'est lui que les compilés attendent.
                if (audit.getResponsableEquipe() != null && audit.getResponsableEquipe().getId().equals(a.getId())) {
                    e.compiles += constats.countByAuditIdAndStatutIn(audit.getId(), List.of(StatutConstat.COMPILE));
                }
            }
        } else {
            // Hors du vivier, l'appelant décide pour la direction : tout compilé l'attend.
            for (Audit audit : enCours) {
                e.compiles += constats.countByAuditIdAndStatutIn(audit.getId(), List.of(StatutConstat.COMPILE));
            }
        }

        e.signatures = signatures.findAllBySignataireIdAndStatut(moi, StatutSignature.EN_ATTENTE).stream()
                .filter(s -> directionId.equals(s.getAudit().getDirectionId()))
                .filter(s -> s.getAudit().getStatut() != StatutAudit.ANNULE)
                .count();
        e.ecarts = notations.publieesDeLaDirection(directionId).stream()
                .filter(n -> n.constatRetenu() == ConstatRQAPBF.NC && n.getNonConformiteId() == null)
                .count();
        e.checklistsBrouillon = checklists.countByDirectionIdAndStatut(directionId, StatutChecklist.BROUILLON);
        return e;
    }

    private static final class Etat {
        private long aValider;
        private long enRetard;
        private long demarrentBientot;
        private long pointsASaisir;
        private long brouillons;
        private long compiles;
        private long signatures;
        private long ecarts;
        private long checklistsBrouillon;
    }
}
