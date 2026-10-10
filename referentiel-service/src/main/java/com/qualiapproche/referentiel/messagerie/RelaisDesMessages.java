package com.qualiapproche.referentiel.messagerie;

import com.qualiapproche.common.messagerie.BoiteDEnvoi;
import org.springframework.stereotype.Component;

/** Le relais de la boîte d'envoi du service. Voir {@link com.qualiapproche.common.messagerie.RelaisDesMessages}. */
@Component
public class RelaisDesMessages extends com.qualiapproche.common.messagerie.RelaisDesMessages {

    public RelaisDesMessages(BoiteDEnvoi<MessageSortant> boite) {
        super(boite);
    }
}
