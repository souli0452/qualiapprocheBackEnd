package com.qualiapproche.common.courriel;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un courriel à envoyer, conservé en base jusqu'à sa remise (patron « transactional outbox »).
 *
 * <p>Le courriel est déposé <b>déjà rendu</b> — objet et corps HTML — : le rejouer après un
 * redémarrage ne demande ni gabarit ni variables. Chaque service a sa table
 * {@code courriel_sortant}, dans sa propre base ; le nom est le même partout, ce qui permet à
 * {@link CourrielSortantRepository} d'écrire une seule fois la sélection des courriels échus.</p>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractCourrielSortant {

    @Id
    private UUID id;

    @Column(nullable = false, length = 320)
    private String destinataire;

    @Column(length = 320)
    private String copie;

    @Column(nullable = false, length = 500)
    private String sujet;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String corps;

    /** D'où vient le courriel (« relance-plan », « publication-constats »…) : pour le diagnostic. */
    @Column(length = 128)
    private String origine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutCourriel statut;

    @Column(nullable = false)
    private int tentatives;

    @Column(name = "prochaine_tentative_at", nullable = false)
    private LocalDateTime prochaineTentativeAt;

    @Column(name = "derniere_erreur", columnDefinition = "TEXT")
    private String derniereErreur;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "envoye_le")
    private LocalDateTime envoyeLe;
}
