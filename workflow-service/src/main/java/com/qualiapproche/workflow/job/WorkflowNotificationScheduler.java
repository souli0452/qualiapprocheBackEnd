package com.qualiapproche.workflow.job;

import com.qualiapproche.workflow.service.WorkflowNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Rejeu périodique des notifications de transition non remises.
 *
 * <p>C'est ce qui rend la remise durable : si le service destinataire était indisponible au moment
 * de la transition, la notification est reprise ici jusqu'à aboutir, au lieu d'être perdue.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowNotificationScheduler {

    /** Borne le lot traité par passage, pour qu'un arriéré ne monopolise pas l'ordonnanceur. */
    private static final int TAILLE_LOT = 50;

    private final WorkflowNotificationService notificationService;

    @Scheduled(fixedDelayString = "${workflow.notifications.rejeu-delai-ms:60000}")
    public void rejouerNotificationsEnAttente() {
        List<UUID> aRejouer = notificationService.notificationsARejouer(TAILLE_LOT);
        if (aRejouer.isEmpty()) {
            return;
        }

        log.info("Rejeu de {} notification(s) de workflow en attente", aRejouer.size());
        int remises = 0;
        for (UUID notificationId : aRejouer) {
            if (notificationService.remettre(notificationId)) {
                remises++;
            }
        }
        log.info("Rejeu terminé : {}/{} notification(s) remise(s)", remises, aRejouer.size());
    }
}
