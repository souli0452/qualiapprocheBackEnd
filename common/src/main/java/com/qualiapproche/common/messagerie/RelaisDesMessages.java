package com.qualiapproche.common.messagerie;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * Le relais d'un service : publie ce que sa boîte d'envoi n'a pas pu publier tout de suite, et
 * purge ce qui est parti.
 *
 * <p>Chaque service en déclare une sous-classe {@code @Component}. Les deux tâches sont sous
 * verrou ShedLock, nommé d'après le service : une seule instance relaie. Le service doit activer
 * la planification ({@code @EnableScheduling}).</p>
 *
 * <p>Réglages : {@code qualisira.messagerie.relais-ms} (10 s), {@code lot} (100),
 * {@code conservation-jours} (30), {@code conservation-confidentielle-heures} (168 : la durée de
 * vie d'un lien de vérification), {@code purge-cron} (3 h 15).</p>
 */
public abstract class RelaisDesMessages {

    private final BoiteDEnvoi<?> boite;

    @Value("${qualisira.messagerie.lot:100}")
    private int lot;

    @Value("${qualisira.messagerie.conservation-jours:30}")
    private int conservationJours;

    @Value("${qualisira.messagerie.conservation-confidentielle-heures:168}")
    private int conservationConfidentielleHeures;

    protected RelaisDesMessages(BoiteDEnvoi<?> boite) {
        this.boite = boite;
    }

    @Scheduled(fixedDelayString = "${qualisira.messagerie.relais-ms:10000}",
            initialDelayString = "${qualisira.messagerie.premier-relais-ms:15000}")
    @SchedulerLock(name = "${spring.application.name}-relais-des-messages",
            lockAtMostFor = "PT10M", lockAtLeastFor = "PT2S")
    public void relayer() {
        boite.tournee(lot);
    }

    @Scheduled(cron = "${qualisira.messagerie.purge-cron:0 15 3 * * *}")
    @SchedulerLock(name = "${spring.application.name}-purge-des-messages", lockAtMostFor = "PT30M")
    public void purger() {
        boite.purger(Duration.ofDays(conservationJours));
        boite.purgerLesConfidentiels(Duration.ofHours(conservationConfidentielleHeures));
    }
}
