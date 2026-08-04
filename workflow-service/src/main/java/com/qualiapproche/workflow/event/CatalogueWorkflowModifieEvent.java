package com.qualiapproche.workflow.event;

import org.springframework.context.ApplicationEvent;

/**
 * Signale qu'un circuit vient d'être créé, modifié ou supprimé.
 *
 * <p>Sert exclusivement à différer le rechargement du catalogue du moteur jusqu'après le commit :
 * mené dans la transaction, il exposait le moteur à des données non encore acquises, et une
 * annulation ultérieure lui laissait un catalogue décrivant des circuits inexistants.</p>
 */
public class CatalogueWorkflowModifieEvent extends ApplicationEvent {

    public CatalogueWorkflowModifieEvent(Object source) {
        super(source);
    }
}
