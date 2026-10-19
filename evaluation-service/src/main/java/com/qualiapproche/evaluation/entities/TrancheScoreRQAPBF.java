package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Une tranche de la grille qui traduit le score global RQAP-BF en niveau, mention et étoiles.
 *
 * <p>Une tranche va de son {@code scoreMin}, inclus, au {@code scoreMin} de la suivante, exclu ;
 * la dernière va jusqu'à 100. La première commence toujours à 0 : aucun score ne reste sans
 * mention. Les cinq tranches d'une direction ne se créent ni ne se suppriment, seuls leurs bornes
 * et libellés se modifient (recette du 17 oct. 2026, observation 28 : la grille officielle reste à
 * recevoir, celle-ci en tient lieu par défaut).</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tranche_score_rqapbf")
public class TrancheScoreRQAPBF extends AuditEntity {

    /** De 1, la plus basse, à 5. */
    @Column(name = "rang", nullable = false)
    private int rang;

    @Column(name = "score_min", nullable = false)
    private double scoreMin;

    @Column(name = "niveau", nullable = false, length = 100)
    private String niveau;

    @Column(name = "mention", nullable = false)
    private String mention;

    @Column(name = "etoiles", nullable = false)
    private int etoiles;

    /** La borne haute, exclue — 100, incluse, pour la dernière : celle de la tranche suivante, pour l'écran. */
    @Transient
    private Double scoreMax;
}
