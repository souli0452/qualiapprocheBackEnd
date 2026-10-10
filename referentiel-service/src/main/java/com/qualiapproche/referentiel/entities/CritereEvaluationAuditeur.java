package com.qualiapproche.referentiel.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class CritereEvaluationAuditeur extends AuditEntity implements Retirable {

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private boolean actif = true;
}
