package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Pour une direction, ce qu'un niveau de la grille RQAP-BF retient : un écart, transmis au module
 * Non-conformités, ou non.
 *
 * <p>La maquette voulait tout ce qui n'est pas « Conforme » en écart ; la recette du 17 oct. 2026
 * a vu une seule auto-évaluation ouvrir 250 non-conformités, « Acceptable » compris — un niveau
 * qui dit « action formalisée et réalisée ». La règle se paramètre donc, niveau par niveau : par
 * défaut, « Non conforme » et « À améliorer » ouvrent une non-conformité, « Acceptable » non.
 * « Conforme » et « Non applicable » n'en ouvrent jamais et ne figurent pas ici.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "regle_ecart_rqapbf")
public class RegleEcartRQAPBF extends AuditEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false, length = 32)
    private NiveauNotationRQAPBF niveau;

    @Column(name = "ouvre_non_conformite", nullable = false)
    private boolean ouvreNonConformite;

    /** Le libellé du niveau, pour l'écran : la grille officielle, avec son barème. */
    public String getLibelle() {
        return niveau == null ? null : niveau.getLibelle();
    }

    /** L'ordre de la grille : du plus bas au plus haut. */
    public int getOrdre() {
        return niveau == null ? 0 : niveau.ordinal() + 1;
    }
}
