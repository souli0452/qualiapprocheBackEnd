package com.qualiapproche.common.messagerie;

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
 * Un message à publier, conservé en base jusqu'à sa remise au courtier (patron « transactional
 * outbox »).
 *
 * <p>Déposé dans la transaction métier, il n'existe que si elle est validée ; publié ensuite, il
 * survit à un redémarrage du service comme à une indisponibilité du courtier. Chaque service a sa
 * table {@code message_sortant}, dans sa propre base ; le nom est le même partout, ce qui permet à
 * {@link MessageSortantRepository} d'écrire une seule fois la sélection des messages échus.</p>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractMessageSortant {

    @Id
    private UUID id;

    /** La file de destination ({@link AdressesMessagerie}). */
    @Column(nullable = false, length = 200)
    private String destination;

    /** Le type logique du message ({@link AdressesMessagerie#TYPE_COURRIEL}…). */
    @Column(nullable = false, length = 64)
    private String type;

    /** Le message, sérialisé en JSON au dépôt : ce qui part est ce qui a été décidé. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutMessage statut;

    @Column(nullable = false)
    private int tentatives;

    @Column(name = "prochaine_tentative_at", nullable = false)
    private LocalDateTime prochaineTentativeAt;

    @Column(name = "derniere_erreur", columnDefinition = "TEXT")
    private String derniereErreur;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "publie_le")
    private LocalDateTime publieLe;
}
