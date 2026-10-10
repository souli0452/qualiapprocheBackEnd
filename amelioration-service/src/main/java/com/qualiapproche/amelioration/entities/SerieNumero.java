package com.qualiapproche.amelioration.entities;

import com.qualiapproche.common.serie.AbstractSerieEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Les séries de numérotation du service (non-conformités). Voir {@link AbstractSerieEntity}. */
@Entity
@Table(name = "serie_numero")
public class SerieNumero extends AbstractSerieEntity {
}
