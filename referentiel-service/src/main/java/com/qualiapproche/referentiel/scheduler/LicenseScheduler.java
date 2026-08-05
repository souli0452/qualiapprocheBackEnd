package com.qualiapproche.referentiel.scheduler;

import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.referentiel.entities.AbonnementDirection;
import com.qualiapproche.referentiel.repository.AbonnementDirectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseScheduler {

    private final AbonnementDirectionRepository abonnementDirectionRepository;
    private final SendMailService sendMailService;

    @Scheduled(cron = "0 0 0 * * *") // Chaque jour à minuit
    public void checkLicenseExpirations() {
        log.info("Checking license expirations...");
        List<AbonnementDirection> abonnements = abonnementDirectionRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (AbonnementDirection abo : abonnements) {
            if (abo.getDateFin() != null) {
                long daysRemaining = ChronoUnit.DAYS.between(now, abo.getDateFin());

                if (daysRemaining == 30 || daysRemaining == 15 || daysRemaining == 7 || daysRemaining == 3 || daysRemaining == 1) {
                    sendExpirationAlert(abo, daysRemaining);
                } else if (daysRemaining == 0) {
                    sendExpirationAlert(abo, 0);
                } else if (daysRemaining < 0 && daysRemaining >= -7) {
                    log.warn("License for direction {} is in grace period ({} days remaining)",
                        abo.getDirection().getLibelleLong(), 7 + daysRemaining);
                }
            }
        }
    }

    private void sendExpirationAlert(AbonnementDirection abo, long days) {
        String directionName = abo.getDirection().getLibelleLong();
        String recipientEmail = abo.getDirection().getEmail(); // On envoie à l'email de la direction (Super Admin)

        String subject = "Alerte : Expiration de votre licence QualiSira";
        String message = days == 0
            ? "Votre licence expire aujourd'hui. Une période de grâce de 7 jours est activée."
            : "Votre licence expire dans " + days + " jours. Pensez à la renouveler.";

        log.info("Sending expiration alert for {}: {} days remaining", directionName, days);

        try {
            // Utilisation d'un template générique si disponible, sinon mail simple
            sendMailService.sendMail(recipientEmail, subject, message, false);
        } catch (Exception e) {
            log.error("Failed to send license expiration email to {}", recipientEmail, e);
        }
    }
}
