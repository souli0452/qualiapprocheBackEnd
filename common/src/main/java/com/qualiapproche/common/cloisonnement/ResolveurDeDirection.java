package com.qualiapproche.common.cloisonnement;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/** Donne à Hibernate la direction de chaque session qu'il ouvre ({@link Cloisonnement#courante()}). */
public class ResolveurDeDirection implements CurrentTenantIdentifierResolver<Object> {

    @Override
    public Object resolveCurrentTenantIdentifier() {
        return Cloisonnement.courante();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(Object direction) {
        return Cloisonnement.estLaRacine(direction);
    }
}
