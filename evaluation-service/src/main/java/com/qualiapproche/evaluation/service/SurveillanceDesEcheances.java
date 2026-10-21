package com.qualiapproche.evaluation.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.RegleNotification;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.entities.RappelEcheance;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.RappelEcheanceRepository;
import com.qualiapproche.evaluation.repository.RegleNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * La ronde de nuit sur le programme d'audit.
 *
 * <p>Deux gestes : poser {@link StatutAudit#EN_RETARD} sur les audits validés jamais démarrés
 * dont la date de début est passée, et relever les règles de notification qui tombent ce jour.</p>
 *
 * <p>Les échéances relevées sont <b>tracées, pas envoyées</b> : chacune devient un
 * {@link RappelEcheance}, que le tableau de bord affiche. evaluation-service n'a pas encore de
 * configuration de messagerie, et l'y ajouter exige des variables de serveur (MAIL_PASSWORD…) qui
 * relèvent du déploiement. Brancher l'envoi est l'étape suivante.</p>
 *
 * <p>Un rappel ne s'inscrit qu'une fois par audit, règle et jour : relancer la ronde — un
 * redémarrage du service — ne le double pas.</p>
 *
 * <p>Sans jeton à cette heure-là, la ronde parcourt toutes les directions : elle ne lit que les
 * dates et les statuts, et les règles de chaque direction ne s'appliquent qu'à ses audits.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SurveillanceDesEcheances {

    private final AuditRepository audits;
    private final RegleNotificationRepository regles;
    private final RappelEcheanceRepository rappels;

    @Scheduled(cron = "${qualisira.audit.surveillance-cron:0 30 5 * * *}")
    @SchedulerLock(name = "evaluation-surveillance-des-echeances", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    @Transactional
    public void ronde() {
        LocalDate aujourdHui = LocalDate.now();
        List<Audit> vivants = audits.findAllByStatutIn(EnumSet.of(
                StatutAudit.PLANIFIE, StatutAudit.EN_PREPARATION, StatutAudit.EN_RETARD, StatutAudit.EN_COURS));

        long marques = vivants.stream().filter(a -> a.marquerEnRetardSiEcheance(aujourdHui)).count();
        if (marques > 0) {
            log.info("{} audit(s) validé(s) mais non démarré(s) passent en retard.", marques);
        }

        Map<UUID, List<RegleNotification>> parDirection = regles.findAll().stream()
                .filter(RegleNotification::isActif)
                .filter(r -> r.getDirectionId() != null)
                .collect(Collectors.groupingBy(RegleNotification::getDirectionId));
        for (Audit audit : vivants) {
            for (RegleNotification regle : parDirection.getOrDefault(audit.getDirectionId(), List.of())) {
                if (regle.declencherSiEcheance(audit, aujourdHui)
                        && !rappels.existsByAuditIdAndRegleIdAndDateRappel(audit.getId(), regle.getId(), aujourdHui)) {
                    rappels.save(RappelEcheance.builder()
                            .directionId(audit.getDirectionId())
                            .auditId(audit.getId())
                            .auditReference(audit.getReference())
                            .regleId(regle.getId())
                            .regleLibelle(regle.getLibelle())
                            .delaiJours(regle.getDelaiJours())
                            .destinataires(regle.getDestinataires())
                            .dateRappel(aujourdHui)
                            .createdAt(LocalDateTime.now())
                            .build());
                    log.info("Échéance d'audit : règle « {} » pour {} (début le {}), destinataires : {}.",
                            regle.getLibelle(), audit.getReference(), audit.getDateDebutPrevue(),
                            regle.getDestinataires());
                }
            }
        }
    }
}
