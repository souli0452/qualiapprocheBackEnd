package com.qualiapproche.support.model;

import com.qualiapproche.common.serie.AbstractSerieEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Les séries de numérotation du service (documents qualité). Voir {@link AbstractSerieEntity}. */
@Entity
@Table(name = "serie_numero")
public class SerieNumero extends AbstractSerieEntity {
}
