package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Un critère sur lequel l'équipe évalue un auditeur à l'issue d'un audit. */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "critere_evaluation_auditeur")
public class CritereEvaluationAuditeur extends AuditEntity {

    @Column(name = "libelle", nullable = false)
    private String libelle;
}
