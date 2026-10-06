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

/**
 * Un niveau de l'échelle d'évaluation des auditeurs.
 *
 * <p>Paramétrable, à la place d'une énumération figée : la liste des niveaux peut évoluer sans
 * modification du code. {@code ordre} en fait aussi la valeur — c'est sur lui que se calcule la
 * moyenne d'un auditeur.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "niveau_evaluation_auditeur")
public class NiveauEvaluationAuditeur extends AuditEntity {

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "ordre", nullable = false)
    private int ordre;
}
