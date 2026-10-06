package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * La note d'un auditeur sur un critère, à l'issue d'un audit.
 *
 * <p>Une ligne par (audit, auditeur évalué, critère) : la granularité par critère vit directement
 * ici. La moyenne d'un auditeur s'obtient en agrégeant ses lignes — voir
 * {@code AuditeurService.scoreEvaluationMoyen}.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "evaluation_auditeur")
public class EvaluationAuditeur extends AuditEntity {

    /** L'audit au titre duquel l'équipe est évaluée. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditeur_id", nullable = false)
    private Auditeur auditeur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "critere_id", nullable = false)
    private CritereEvaluationAuditeur critere;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "niveau_id", nullable = false)
    private NiveauEvaluationAuditeur niveau;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutEvaluation statut;
}
