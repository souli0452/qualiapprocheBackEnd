package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.EvaluationAuditeurDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.EvaluationAuditeur;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import com.qualiapproche.evaluation.repository.CritereEvaluationAuditeurRepository;
import com.qualiapproche.evaluation.repository.EvaluationAuditeurRepository;
import com.qualiapproche.evaluation.repository.NiveauEvaluationAuditeurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * L'évaluation des auditeurs à l'issue d'un audit, critère par critère.
 *
 * <p>Une ligne par (audit, auditeur, critère) : évaluer à nouveau le même critère corrige la note
 * au lieu d'en ajouter une seconde, qui compterait double dans la moyenne. On n'évalue que les
 * membres de l'équipe, et qu'un audit au moins en cours — avant, ils n'ont rien fait.</p>
 */
@Service
@RequiredArgsConstructor
public class EvaluationAuditeurService {

    private final EvaluationAuditeurRepository depot;
    private final CritereEvaluationAuditeurRepository criteres;
    private final NiveauEvaluationAuditeurRepository niveaux;
    private final AuditService audits;
    private final AuditeurService auditeurs;

    @Transactional(readOnly = true)
    public List<EvaluationAuditeurDto> deLAudit(UUID auditId) {
        return depot.findAllByAuditId(audits.sienOuRien(auditId).getId()).stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationAuditeurDto> deLAuditeur(UUID auditeurId) {
        return depot.findAllByAuditeurId(auditeurs.sienOuRien(auditeurId).getId()).stream().map(this::versDto).toList();
    }

    @Transactional
    public EvaluationAuditeurDto evaluer(EvaluationAuditeurDto saisie) {
        Audit audit = audits.sienOuRien(saisie.getAuditId());
        if (audit.getStatut() != StatutAudit.EN_COURS && audit.getStatut() != StatutAudit.CLOTURE) {
            throw new BusinessException("On évalue l'équipe d'un audit mené : " + audit.getReference()
                    + " est " + audit.getStatut() + ".", HttpStatus.CONFLICT);
        }
        Auditeur auditeur = auditeurs.sienOuRien(saisie.getAuditeurId());
        if (!audit.aDansLEquipe(auditeur)) {
            throw new BusinessException("Cet auditeur ne faisait pas partie de l'équipe de "
                    + audit.getReference() + ".", HttpStatus.BAD_REQUEST);
        }
        EvaluationAuditeur evaluation = depot.findByAuditIdAndAuditeurIdAndCritereId(
                        audit.getId(), auditeur.getId(), saisie.getCritereId())
                .orElseGet(() -> EvaluationAuditeur.builder()
                        .audit(audit)
                        .auditeur(auditeur)
                        .critere(Perimetre.sienneOuRien(criteres, saisie.getCritereId(), "critère d'évaluation"))
                        .statut(StatutEvaluation.BROUILLON)
                        .build());
        if (evaluation.getStatut() == StatutEvaluation.VALIDEE) {
            throw new BusinessException("Cette évaluation est validée : elle ne se modifie plus.", HttpStatus.CONFLICT);
        }
        evaluation.setNiveau(Perimetre.sienneOuRien(niveaux, saisie.getNiveauId(), "niveau d'évaluation"));
        evaluation.setCommentaire(saisie.getCommentaire());
        evaluation.setDateEvaluation(LocalDate.now());
        return versDto(depot.save(evaluation));
    }

    @Transactional
    public EvaluationAuditeurDto valider(UUID id) {
        EvaluationAuditeur evaluation = Perimetre.sienneOuRien(depot, id, "évaluation d'auditeur");
        evaluation.setStatut(StatutEvaluation.VALIDEE);
        return versDto(depot.save(evaluation));
    }

    private EvaluationAuditeurDto versDto(EvaluationAuditeur e) {
        return EvaluationAuditeurDto.builder()
                .id(e.getId())
                .auditId(e.getAudit().getId())
                .auditeurId(e.getAuditeur().getId())
                .critereId(e.getCritere().getId())
                .critereLibelle(e.getCritere().getLibelle())
                .niveauId(e.getNiveau().getId())
                .niveauLibelle(e.getNiveau().getLibelle())
                .dateEvaluation(e.getDateEvaluation())
                .commentaire(e.getCommentaire())
                .statut(e.getStatut())
                .build();
    }
}
