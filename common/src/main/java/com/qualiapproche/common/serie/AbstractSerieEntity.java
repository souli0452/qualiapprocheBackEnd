package com.qualiapproche.common.serie;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Une série de numérotation : le dernier numéro attribué dans une portée donnée.
 *
 * <p>Repris du patron de sigatt (immatriculation). La clé {@code cleSerie} dit sur quoi porte
 * l'unicité — « NC|structure|2026 », « DOC|PRO-DQA-2026 » — et {@code valeurCourante} ne recule
 * jamais. Chaque service a sa propre table {@code serie_numero}, dans sa propre base ; le nom de
 * table est le même partout, ce qui permet à {@link SerieRepository} d'écrire une seule fois la
 * création atomique d'une série.</p>
 *
 * <p>Ni audit ni suppression : une série ne s'efface pas, et un numéro attribué non plus.</p>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractSerieEntity {

    @Id
    @Column(name = "cle_serie", length = 128, nullable = false)
    private String cleSerie;

    @Column(name = "valeur_courante", nullable = false)
    private long valeurCourante;
}
