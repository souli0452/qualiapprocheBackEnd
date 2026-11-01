package com.qualiapproche.common.cloisonnement;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;

/** Pose {@link GardeDeChargement} et {@link GardeDInsertion} sur chaque fabrique de sessions (déclaré dans META-INF/services). */
public class IntegrationHibernate implements Integrator {

    @Override
    public void integrate(Metadata metadata, BootstrapContext contexte, SessionFactoryImplementor fabrique) {
        EventListenerRegistry registre = fabrique.getServiceRegistry().requireService(EventListenerRegistry.class);
        registre.appendListeners(EventType.POST_LOAD, new GardeDeChargement());
        registre.appendListeners(EventType.PRE_INSERT, new GardeDInsertion());
    }

    @Override
    public void disintegrate(SessionFactoryImplementor fabrique,
                             org.hibernate.service.spi.SessionFactoryServiceRegistry registre) {
        // rien à défaire
    }
}
