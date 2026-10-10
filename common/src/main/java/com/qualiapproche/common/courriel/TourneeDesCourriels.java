package com.qualiapproche.common.courriel;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * La tournée qui remet les courriels d'un service, et la purge de ceux qui sont partis.
 *
 * <p>Chaque service en déclare une sous-classe {@code @Component}, sans rien y écrire d'autre. Les
 * deux tâches sont sous verrou ShedLock, nommé d'après le service : une seule instance fait la
 * tournée, et une tournée qui dure n'est pas doublée par la suivante. Le service doit activer la
 * planification ({@code @EnableScheduling}).</p>
 *
 * <p>Réglages ({@code application.yml}) : {@code qualisira.courriels.tournee-ms} (15 s),
 * {@code lot} (50 par tournée), {@code conservation-jours} (30), {@code purge-cron} (3 h 15).</p>
 */
public abstract class TourneeDesCourriels {

    private final FileDeCourriels<?> file;

    @Value("${qualisira.courriels.lot:50}")
    private int lot;

    @Value("${qualisira.courriels.conservation-jours:30}")
    private int conservationJours;

    protected TourneeDesCourriels(FileDeCourriels<?> file) {
        this.file = file;
    }

    @Scheduled(fixedDelayString = "${qualisira.courriels.tournee-ms:15000}",
            initialDelayString = "${qualisira.courriels.premiere-tournee-ms:20000}")
    @SchedulerLock(name = "${spring.application.name}-tournee-des-courriels",
            lockAtMostFor = "PT10M", lockAtLeastFor = "PT5S")
    public void tournee() {
        file.tournee(lot);
    }

    @Scheduled(cron = "${qualisira.courriels.purge-cron:0 15 3 * * *}")
    @SchedulerLock(name = "${spring.application.name}-purge-des-courriels", lockAtMostFor = "PT30M")
    public void purge() {
        file.purger(Duration.ofDays(conservationJours));
    }
}
