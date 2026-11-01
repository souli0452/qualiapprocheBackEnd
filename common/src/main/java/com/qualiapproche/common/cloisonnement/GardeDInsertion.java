package com.qualiapproche.common.cloisonnement;

import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.springframework.security.access.AccessDeniedException;

/**
 * Un compte rattaché à aucun client n'écrit rien qui soit à un client.
 *
 * <p>Sans cette garde, la ligne recevrait la direction {@link Cloisonnement#AUCUNE} et ne serait
 * plus visible de personne — pas même de son auteur, qui croirait l'avoir enregistrée.</p>
 */
public class GardeDInsertion implements PreInsertEventListener {

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        if (event.getEntity() instanceof Cloisonnable
                && Cloisonnement.AUCUNE.equals(event.getSession().getTenantIdentifierValue())) {
            throw new AccessDeniedException("Votre compte n'est rattaché à aucun client : il ne peut rien "
                    + "enregistrer.");
        }
        return false;
    }
}
