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
import com.qualiapproche.evaluation.service.referentiel.CodificationDocumentService;
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
            activites.add(ActivitePlan.builder()
                    .plan(plan)
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

    @Transactional
    public PlanAuditDto valider(UUID auditId) {
        PlanAudit plan = sienOuRien(auditId);
        plan.valider();
        return versDto(depot.save(plan));
    }

    @Transactional
    public PlanAuditDto partagerAvecAudite(UUID auditId) {
        PlanAudit plan = sienOuRien(auditId);
        plan.partagerAvecAudite();
        return versDto(depot.save(plan));
    }

    /** Charge un plan reçu tout fait ; le plan est créé s'il n'existait pas encore. */
    @Transactional
    public PlanAuditDto charger(UUID auditId, MultipartFile fichier) {
        Audit audit = audits.sienOuRien(auditId);
        PlanAudit plan = depot.findByAuditId(audit.getId()).orElseGet(() -> amorcer(audit));
        plan.exigerModifiable();
        plan.setFichierRecu(fichiers.deposer(fichier, audit.getId().toString(), "plan"));
        plan.setOrigine(OrigineDocument.CHARGE);
        return versDto(depot.save(plan));
    }

    @Transactional(readOnly = true)
    public InputStream contenuDuFichier(UUID auditId) {
        return fichiers.contenu(sienOuRien(auditId).getFichierRecu());
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
                .statut(plan.getStatut())
                .datePartage(plan.getDatePartage())
                .activites(plan.getActivites().stream().map(a -> ActivitePlanDto.builder()
                        .id(a.getId())
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
