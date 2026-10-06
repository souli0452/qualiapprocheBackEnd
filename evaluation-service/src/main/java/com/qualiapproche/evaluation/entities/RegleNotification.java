package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Une alerte d'échéance : « prévenir N jours avant le début d'un audit ».
 *
 * <p>Les règles valent pour tous les audits de la direction qui les pose : elles surveillent le
 * programme entier, et non un audit choisi un à un.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "regle_notification")
public class RegleNotification extends AuditEntity {

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "delai_jours", nullable = false)
    private int delaiJours;

    /** Adresses ou rôles à prévenir, séparés par des virgules. */
    @Column(name = "destinataires", columnDefinition = "TEXT")
    private String destinataires;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    /**
     * La règle se déclenche-t-elle aujourd'hui pour cet audit ?
     *
     * <p>Le jour exact seulement, et non « à moins de N jours » : l'alerte part une fois, au lieu
     * de se répéter chaque nuit jusqu'au début de l'audit. Un audit terminé ou sans date de début
     * ne déclenche rien.</p>
     */
    public boolean declencherSiEcheance(Audit audit, LocalDate dateJour) {
        if (!actif || audit == null || audit.getDateDebutPrevue() == null
                || audit.getStatut() == null || audit.getStatut().estTermine()) {
            return false;
        }
        return ChronoUnit.DAYS.between(dateJour, audit.getDateDebutPrevue()) == delaiJours;
    }
}
