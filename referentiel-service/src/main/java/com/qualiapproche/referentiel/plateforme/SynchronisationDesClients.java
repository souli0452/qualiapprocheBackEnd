package com.qualiapproche.referentiel.plateforme;

import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.service.TenantProvisioningService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tient chaque service à jour des clients : d'abord le rattrapage (les lignes écrites avant le
 * cloisonnement rendues à leur client), puis l'amorce du paramétrage de chacun.
 *
 * <p>L'ordre compte : avant le rattrapage, la colonne {@code direction_id} d'un service porte
 * encore des identifiants de structures ; amorcer d'abord y poserait un paramétrage en double.</p>
 *
 * <ul>
 *   <li>au démarrage, et toutes les deux minutes tant qu'un service n'a pas répondu — les services
 *       ne démarrent pas tous ensemble ;</li>
 *   <li>chaque nuit : le complément qu'une nouvelle version d'un service apporte à son paramétrage
 *       (une permission, un modèle de courriel) parvient ainsi à tous les clients, même quand ce
 *       service a été redéployé seul.</li>
 * </ul>
 */
@Slf4j
@Component
public class SynchronisationDesClients {

    private final GestionDesClients gestion;
    private final TenantProvisioningService provisionnement;
    private final AtomicBoolean aJour = new AtomicBoolean(false);

    public SynchronisationDesClients(GestionDesClients gestion, TenantProvisioningService provisionnement) {
        this.gestion = gestion;
        this.provisionnement = provisionnement;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 120_000)
    @SchedulerLock(name = "referentiel-synchronisation-des-clients", lockAtMostFor = "PT10M")
    public void tantQueNecessaire() {
        if (!aJour.get()) {
            aJour.set(synchroniser());
        }
    }

    @Scheduled(cron = "0 30 2 * * *")
    @SchedulerLock(name = "referentiel-synchronisation-nocturne", lockAtMostFor = "PT30M")
    public void chaqueNuit() {
        synchroniser();
    }

    /** @return vrai si chaque service a répondu */
    public boolean synchroniser() {
        Map<String, Object> rattrapage = gestion.rattraper();
        if (GestionDesClients.incomplet(rattrapage)) {
            log.info("Synchronisation des clients reportée, un service n'a pas répondu : {}", rattrapage);
            return false;
        }
        boolean complet = true;
        for (Structure client : gestion.directions()) {
            Map<String, Object> echecs = gestion.amorcer(client.getId());
            if (!echecs.isEmpty()) {
                complet = false;
                log.warn("Amorce du client « {} » incomplète : {}", client.getLibelleLong(), echecs);
            }
        }
        // Les comptes du fichier de livraison, maintenant que leurs rôles existent chez leur client.
        complet &= provisionnement.creerLesComptesEnAttente(gestion);
        log.info("Synchronisation des clients {} (rattrapage : {}).", complet ? "faite" : "incomplète", rattrapage);
        return complet;
    }
}
