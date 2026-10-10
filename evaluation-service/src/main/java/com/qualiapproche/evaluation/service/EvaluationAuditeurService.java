package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.dto.CritereEvaluationAuditeurDto;
import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.client.Parametrage;
import com.qualiapproche.evaluation.dto.EvaluationAuditeurDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.EvaluationAuditeur;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import com.qualiapproche.evaluation.repository.EvaluationAuditeurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private final Parametrage parametrage;
    private final AuditService audits;
    private final AuditeurService auditeurs;

    @Transactional(readOnly = true)
    public List<EvaluationAuditeurDto> deLAudit(UUID auditId) {
        return versDtos(depot.findAllByAuditId(audits.sienOuRien(auditId).getId()));
    }

    @Transactional(readOnly = true)
    public List<EvaluationAuditeurDto> deLAuditeur(UUID auditeurId) {
        return versDtos(depot.findAllByAuditeurId(auditeurs.sienOuRien(auditeurId).getId()));
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
                        .critereId(parametrage.critereActif(saisie.getCritereId()).getId())
                        .statut(StatutEvaluation.BROUILLON)
                        .build());
        if (evaluation.getStatut() == StatutEvaluation.VALIDEE) {
            throw new BusinessException("Cette évaluation est validée : elle ne se modifie plus.", HttpStatus.CONFLICT);
        }
        NiveauEvaluationAuditeurDto niveau = parametrage.niveauActif(saisie.getNiveauId());
        evaluation.setNiveauId(niveau.getId());
        evaluation.setValeurNiveau(niveau.getOrdre());
        evaluation.setCommentaire(saisie.getCommentaire());
        evaluation.setDateEvaluation(LocalDate.now());
        return versDtos(List.of(depot.save(evaluation))).get(0);
    }

    @Transactional
    public EvaluationAuditeurDto valider(UUID id) {
        EvaluationAuditeur evaluation = Perimetre.sienneOuRien(depot, id, "évaluation d'auditeur");
        evaluation.setStatut(StatutEvaluation.VALIDEE);
        return versDtos(List.of(depot.save(evaluation))).get(0);
    }

    /** Les libellés viennent du référentiel, lus une fois pour toute la liste. */
    private List<EvaluationAuditeurDto> versDtos(List<EvaluationAuditeur> evaluations) {
        if (evaluations.isEmpty()) {
            return List.of();
        }
        Map<UUID, CritereEvaluationAuditeurDto> criteres = parametrage.criteresPourAffichage();
        Map<UUID, NiveauEvaluationAuditeurDto> niveaux = parametrage.niveauxPourAffichage();
        return evaluations.stream().map(e -> versDto(e, criteres.get(e.getCritereId()), niveaux.get(e.getNiveauId())))
                .toList();
    }

    private static EvaluationAuditeurDto versDto(EvaluationAuditeur e, CritereEvaluationAuditeurDto critere,
                                                 NiveauEvaluationAuditeurDto niveau) {
        return EvaluationAuditeurDto.builder()
                .id(e.getId())
                .auditId(e.getAudit().getId())
                .auditeurId(e.getAuditeur().getId())
                .critereId(e.getCritereId())
                .critereLibelle(critere == null ? null : critere.getLibelle())
                .niveauId(e.getNiveauId())
                .niveauLibelle(niveau == null ? null : niveau.getLibelle())
                .dateEvaluation(e.getDateEvaluation())
                .commentaire(e.getCommentaire())
                .statut(e.getStatut())
                .build();
    }
}
