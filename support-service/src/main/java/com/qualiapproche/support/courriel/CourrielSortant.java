package com.qualiapproche.support.courriel;

import com.qualiapproche.common.courriel.AbstractCourrielSortant;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Les courriels du service, conservés jusqu'à leur remise. Voir {@link AbstractCourrielSortant}. */
@Entity
@Table(name = "courriel_sortant")
public class CourrielSortant extends AbstractCourrielSortant {
}
