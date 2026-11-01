package com.qualiapproche.support.job;

import com.qualiapproche.common.cloisonnement.ExecutionCloisonnee;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import com.qualiapproche.support.service.QmsAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QmsRevisionAlertScheduler {

    private final DocumentQmsRepository documentRepository;
    private final QmsAuditLogService auditLogService;
    private final ExecutionCloisonnee execution;

    /**
     * Executes daily at 02:00 AM.
     *
     * <p>La liste est lue pour toute l'installation, puis chaque document est traité dans sa
     * propre transaction, sous la direction de son client : l'entrée du journal qu'il produit lui
     * revient, au lieu d'échoir à l'installation où aucun client ne la verrait. Un document en
     * échec n'interrompt plus la tournée des autres.</p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "support-alertes-de-revision", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void checkDocumentRevisionDeadlines() {
        log.info("Starting QMS document revision deadlines check...");

        List<DocumentQms> activeDocs = execution.enInstallation(
                documentRepository::findByEsTraiterTrueAndObsoleteFalseAndArchivedFalse);
        LocalDate today = LocalDate.now();

        for (DocumentQms actif : activeDocs) {
            if (actif.getDateProchRevision() == null) {
                continue;
            }
            try {
                execution.pour(actif.getDirectionId(), () -> documentRepository.findById(actif.getId())
                        .ifPresent(doc -> verifier(doc, today)));
            } catch (Exception e) {
                log.error("Échéance de révision du document {} non vérifiée : {}", actif.getDocumentNumber(),
                        e.getMessage());
            }
        }
    }

    private void verifier(DocumentQms doc, LocalDate today) {
        LocalDate nextRevisionDate = doc.getDateProchRevision().toLocalDate();
        long daysRemaining = ChronoUnit.DAYS.between(today, nextRevisionDate);

        log.debug("Document {} has {} days remaining before revision limit", doc.getDocumentNumber(), daysRemaining);

        if (daysRemaining < 0) {
            log.warn("Document {} is overdue for revision!", doc.getDocumentNumber());

            doc.setEnRetardRevision(true);
            doc.setAlerteEnvoyee("expire");
            documentRepository.save(doc);

            auditLogService.logAction(
                    "ALERTE_REVISION_RETARD",
                    doc.getDocumentNumber(),
                    "Le document est en retard de révision depuis le " + nextRevisionDate
            );

            sendAlertEmail(doc, "CRITIQUE (En retard)", daysRemaining);

        } else if (daysRemaining == 60 || daysRemaining == 30 || daysRemaining == 7 || daysRemaining == 0) {
            String thresholdLabel = "J-" + daysRemaining;
            if (!thresholdLabel.equals(doc.getAlerteEnvoyee())) {
                doc.setAlerteEnvoyee(thresholdLabel);
                documentRepository.save(doc);

                auditLogService.logAction(
                        "ALERTE_REVISION",
                        doc.getDocumentNumber(),
                        "Notification d'alerte de révision à " + daysRemaining + " jours."
                );

                sendAlertEmail(doc, thresholdLabel, daysRemaining);
            }
        }
    }

    private void sendAlertEmail(DocumentQms doc, String thresholdLabel, long daysRemaining) {
        log.info("==========================================================================");
        log.info(" [SMTP ALERT EMAIL SENT]");
        log.info(" From: qms-system@qualapproche.bf");
        log.info(" To: {} (Redacteur), responsable-qualite@qualapproche.bf", doc.getRedacteur());
        log.info(" Subject: [QMS] Révision requise — {} — {} jours restants", doc.getDocumentNumber(), thresholdLabel);
        log.info(" Body: Le document '{}' (Ref: {}) expire le {}.", doc.getDocumentNumber(), doc.getDocumentNumber(), doc.getDateProchRevision());
        log.info("       Veuillez procéder à sa mise à jour.");
        log.info("==========================================================================");
    }
}
