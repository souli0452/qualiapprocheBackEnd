package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.ActivitePlanDto;
import com.qualiapproche.evaluation.dto.PlanAuditDto;
import com.qualiapproche.evaluation.entities.ActivitePlan;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.PlanAudit;
import com.qualiapproche.evaluation.enumeration.OrigineDocument;
import com.qualiapproche.evaluation.enumeration.StatutPlanAudit;
import com.qualiapproche.evaluation.repository.PlanAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Le plan de chaque audit : un seul par audit, rédigé ou reçu, puis partagé avec l'audité.
 *
 * <p>Le plan est amorcé avec les objectifs, la portée et les critères de l'audit : ce sont les
 * mêmes, et les ressaisir n'invite qu'à les faire diverger.</p>
 */
@Service
@RequiredArgsConstructor
public class PlanAuditService {

    private final PlanAuditRepository depot;
    private final AuditService audits;
    private final CodificationDocumentService codification;
    private final FichiersAudit fichiers;
    private final org.springframework.context.ApplicationEventPublisher evenements;

    @Transactional(readOnly = true)
    public PlanAuditDto lire(UUID auditId) {
        return versDto(sienOuRien(auditId));
    }

    /** Pose ou met à jour le plan de l'audit, tant qu'il est en brouillon. */
    @Transactional
    public PlanAuditDto enregistrer(UUID auditId, PlanAuditDto saisie) {
        Audit audit = audits.sienOuRien(auditId);
        if (audit.getStatut().estTermine()) {
            throw new BusinessException("L'audit " + audit.getReference() + " est " + audit.getStatut()
                    + " : son plan ne se modifie plus.", HttpStatus.CONFLICT);
        }
        if (audit.getType() != null && audit.getType().isCircuitExterne()) {
            throw new BusinessException("Le plan d'un audit externe n'est pas élaboré dans le système :"
                    + " il est transmis par l'organisme auditeur, puis chargé.", HttpStatus.CONFLICT);
        }
        PlanAudit plan = depot.findByAuditId(audit.getId()).orElseGet(() -> amorcer(audit));
        plan.exigerModifiable();
        plan.setObjectifs(saisie.getObjectifs());
        plan.setPortee(saisie.getPortee());
        plan.setCriteres(saisie.getCriteres());
        plan.setMethodesAudit(saisie.getMethodesAudit() == null ? new ArrayList<>()
                : new ArrayList<>(saisie.getMethodesAudit()));
        plan.setLieu(saisie.getLieu());
        plan.setLangue(saisie.getLangue());
        plan.setMoyensNecessaires(saisie.getMoyensNecessaires());
        List<ActivitePlan> activites = new ArrayList<>();
        for (ActivitePlanDto a : saisie.getActivites() == null ? List.<ActivitePlanDto>of() : saisie.getActivites()) {
            verifierLeCreneau(audit, a);
            activites.add(ActivitePlan.builder()
                    .plan(plan)
                    .jour(a.getJour())
                    .horaireDebut(a.getHoraireDebut())
                    .horaireFin(a.getHoraireFin())
                    .libelleActivite(a.getLibelleActivite())
                    .processusConcerne(a.getProcessusConcerne())
                    .auditeur(a.getAuditeur())
                    .personnesAuditees(a.getPersonnesAuditees())
                    .build());
        }
        plan.getActivites().clear();
        plan.getActivites().addAll(activites);
        return versDto(depot.save(plan));
    }

    /**
     * Un créneau tient dans la période de l'audit, et finit après avoir commencé : « 09:00 –
     * 08:30 » passait tel quel jusqu'à l'aperçu (recette du 17 oct. 2026, observation 15).
     */
    static void verifierLeCreneau(Audit audit, ActivitePlanDto a) {
        String quoi = a.getLibelleActivite() == null || a.getLibelleActivite().isBlank()
                ? "Une activité" : "L'activité « " + a.getLibelleActivite().strip() + " »";
        java.time.LocalDate jour = a.getJour();
        if (jour != null && audit.getDateDebutPrevue() != null && audit.getDateFinPrevue() != null
                && (jour.isBefore(audit.getDateDebutPrevue()) || jour.isAfter(audit.getDateFinPrevue()))) {
            throw new BusinessException(quoi + " tombe hors de la période de l'audit (du "
                    + audit.getDateDebutPrevue() + " au " + audit.getDateFinPrevue() + ").", HttpStatus.BAD_REQUEST);
        }
        java.time.LocalTime debut = heure(a.getHoraireDebut());
        java.time.LocalTime fin = heure(a.getHoraireFin());
        if (debut != null && fin != null && !fin.isAfter(debut)) {
            throw new BusinessException(quoi + " finit (" + a.getHoraireFin().strip() + ") avant d'avoir commencé ("
                    + a.getHoraireDebut().strip() + ").", HttpStatus.BAD_REQUEST);
        }
    }

    /** L'heure d'un horaire « 08:30 » ; nulle si le champ est vide ou d'une autre forme. */
    private static java.time.LocalTime heure(String horaire) {
        if (horaire == null || horaire.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalTime.parse(horaire.strip());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    @Transactional
    public PlanAuditDto valider(UUID auditId) {
        PlanAudit plan = sienOuRien(auditId);
        // L'audit d'abord, son plan ensuite : un plan se validait avant l'audit (observation 18).
        com.qualiapproche.evaluation.enumeration.StatutAudit statut = plan.getAudit().getStatut();
        if (statut == com.qualiapproche.evaluation.enumeration.StatutAudit.PLANIFIE) {
            throw new BusinessException("Validez d'abord l'audit " + plan.getAudit().getReference()
                    + " : son plan se valide ensuite.", HttpStatus.CONFLICT);
        }
        plan.valider();
        return versDto(depot.save(plan));
    }

    /** Rouvre le plan en brouillon, dans une nouvelle version, tant que l'audit n'est pas terminé. */
    @Transactional
    public PlanAuditDto reviser(UUID auditId) {
        PlanAudit plan = sienOuRien(auditId);
        if (plan.getAudit().getStatut() != null && plan.getAudit().getStatut().estTermine()) {
            throw new BusinessException("L'audit est terminé : son plan ne se révise plus.", HttpStatus.CONFLICT);
        }
        plan.reviser(com.qualiapproche.common.utils.SecurityUtils.getAuteurNom());
        return versDto(depot.save(plan));
    }

    @Transactional
    public PlanAuditDto partagerAvecAudite(UUID auditId) {
        PlanAudit plan = sienOuRien(auditId);
        plan.partagerAvecAudite();
        PlanAuditDto partage = versDto(depot.save(plan));
        evenements.publishEvent(new AnnoncesDeLAudit.Annonce(plan.getAudit().getId(), AnnoncesDeLAudit.Geste.PARTAGE_PLAN));
        return partage;
    }

    /**
     * Charge le plan qu'un organisme auditeur a transmis ; le plan est créé s'il n'existait pas.
     *
     * <p>Réservé aux audits externes et vivants, comme l'élaboration est réservée aux audits
     * internes : seul l'écran l'empêchait, et un appel direct chargeait un plan sur un audit
     * interne ou clôturé. Un plan reçu ne prend pas de numéro dans la série des plans : il porte
     * celui de l'organisme, et le consommer laissait un trou dans la série.</p>
     */
    @Transactional
    public PlanAuditDto charger(UUID auditId, MultipartFile fichier) {
        Audit audit = audits.sienOuRien(auditId);
        if (audit.getStatut().estTermine()) {
            throw new BusinessException("L'audit " + audit.getReference() + " est " + audit.getStatut()
                    + " : son plan ne se remplace plus.", HttpStatus.CONFLICT);
        }
        if (audit.getType() == null || !audit.getType().isCircuitExterne()) {
            throw new BusinessException("Le plan d'un audit interne s'élabore dans le système :"
                    + " seul le plan d'un audit externe se charge.", HttpStatus.CONFLICT);
        }
        PlanAudit plan = depot.findByAuditId(audit.getId()).orElseGet(() -> amorcerRecu(audit));
        plan.exigerModifiable();
        plan.setFichierRecu(fichiers.deposer(fichier, audit.getId().toString(), "plan"));
        plan.setNomFichierRecu(FichiersAudit.nomDOrigine(fichier));
        plan.setOrigine(OrigineDocument.CHARGE);
        return versDto(depot.save(plan));
    }

    /** Le nom sous lequel rendre le plan chargé, extension comprise. */
    @Transactional(readOnly = true)
    public String nomDuFichier(UUID auditId) {
        PlanAudit plan = sienOuRien(auditId);
        return plan.getNomFichierRecu() != null ? plan.getNomFichierRecu() : "plan-audit";
    }

    @Transactional(readOnly = true)
    public InputStream contenuDuFichier(UUID auditId) {
        return fichiers.contenu(sienOuRien(auditId).getFichierRecu());
    }

    private PlanAudit amorcerRecu(Audit audit) {
        return PlanAudit.builder()
                .audit(audit)
                .objectifs(audit.getObjectifsAudit())
                .portee(audit.getPorteeAudit())
                .criteres(audit.getCriteresAudit())
                .origine(OrigineDocument.CHARGE)
                .statut(StatutPlanAudit.BROUILLON)
                .build();
    }

    private PlanAudit amorcer(Audit audit) {
        return PlanAudit.builder()
                .audit(audit)
                .reference(codification.genererReference(CodificationDocumentService.PLAN_AUDIT))
                .objectifs(audit.getObjectifsAudit())
                .portee(audit.getPorteeAudit())
                .criteres(audit.getCriteresAudit())
                .origine(OrigineDocument.GENERE)
                .statut(StatutPlanAudit.BROUILLON)
                .build();
    }

    private PlanAudit sienOuRien(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        return depot.findByAuditId(audit.getId())
                .orElseThrow(() -> new BusinessException("L'audit " + audit.getReference()
                        + " n'a pas encore de plan.", HttpStatus.NOT_FOUND));
    }

    private PlanAuditDto versDto(PlanAudit plan) {
        return PlanAuditDto.builder()
                .id(plan.getId())
                .auditId(plan.getAudit().getId())
                .reference(plan.getReference())
                .objectifs(plan.getObjectifs())
                .portee(plan.getPortee())
                .criteres(plan.getCriteres())
                .methodesAudit(List.copyOf(plan.getMethodesAudit()))
                .lieu(plan.getLieu())
                .langue(plan.getLangue())
                .moyensNecessaires(plan.getMoyensNecessaires())
                .origine(plan.getOrigine())
                .fichierCharge(plan.getFichierRecu() != null)
                .nomFichierCharge(plan.getNomFichierRecu())
                .statut(plan.getStatut())
                .datePartage(plan.getDatePartage())
                .version(plan.getVersion())
                .reviseLe(plan.getReviseLe())
                .revisePar(plan.getRevisePar())
                .activites(plan.getActivites().stream().map(a -> ActivitePlanDto.builder()
                        .id(a.getId())
                        .jour(a.getJour())
                        .horaireDebut(a.getHoraireDebut())
                        .horaireFin(a.getHoraireFin())
                        .libelleActivite(a.getLibelleActivite())
                        .processusConcerne(a.getProcessusConcerne())
                        .auditeur(a.getAuditeur())
                        .personnesAuditees(a.getPersonnesAuditees())
                        .build()).toList())
                .build();
    }
}
