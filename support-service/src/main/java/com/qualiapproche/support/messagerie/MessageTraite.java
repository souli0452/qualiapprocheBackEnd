package com.qualiapproche.support.messagerie;

import com.qualiapproche.common.messagerie.AbstractMessageTraite;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Les messages déjà traités par ce service. Voir {@link AbstractMessageTraite}. */
@Entity
@Table(name = "message_traite")
public class MessageTraite extends AbstractMessageTraite {
}
