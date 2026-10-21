package com.qualiapproche.evaluation.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une alerte d'échéance relevée par la ronde de nuit : « AUD-2026-013 démarre dans 7 jours ».
 *
 * <p>Le tableau de bord les affiche comme « notifications récentes ». Elles ne sont pas encore
 * envoyées par courriel — voir {@code SurveillanceDesEcheances} —, mais elles sont tracées : ce que
 * la règle a déclenché se lit, au lieu de se perdre dans un journal.</p>
 *
 * <p>Hors d'{@code AuditEntity}, délibérément : celle-ci prend la direction dans le jeton à
 * l'enregistrement, et la ronde de nuit n'en a pas. La direction est posée ici à la main, depuis
 * l'audit.</p>
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "rappel_echeance")
public class RappelEcheance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "direction_id", nullable = false)
    private UUID directionId;

    @Column(name = "audit_id", nullable = false)
    private UUID auditId;

    @Column(name = "audit_reference", nullable = false, length = 60)
    private String auditReference;

    @Column(name = "regle_id", nullable = false)
    private UUID regleId;

    @Column(name = "regle_libelle", nullable = false)
    private String regleLibelle;

    @Column(name = "delai_jours", nullable = false)
    private int delaiJours;

    @Column(name = "destinataires", columnDefinition = "TEXT")
    private String destinataires;

    @Column(name = "date_rappel", nullable = false)
    private LocalDate dateRappel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
