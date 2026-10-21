package com.qualiapproche.workflow.messagerie;

import com.qualiapproche.common.messagerie.AbstractMessageSortant;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** La boîte d'envoi du service. Voir {@link AbstractMessageSortant}. */
@Entity
@Table(name = "message_sortant")
public class MessageSortant extends AbstractMessageSortant {
}
