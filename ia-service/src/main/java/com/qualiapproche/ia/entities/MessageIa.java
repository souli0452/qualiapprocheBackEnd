package com.qualiapproche.ia.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.ia.enumeration.RoleMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Un message d'un fil de conversation.
 *
 * <p>Les deux rôles sont conservés — la question comme la réponse : sans la question, une réponse
 * d'assistant relue six mois plus tard ne veut rien dire, et c'est l'échange entier qui répond à
 * « d'où vient cette idée ».</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "messages_ia")
public class MessageIa extends AuditEntity {

    /** Le fil auquel ce message appartient. */
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    /** Qui parle — inscrit par le serveur, jamais reçu de l'appelant. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RoleMessage role;

    /** Le texte échangé. */
    @Column(name = "contenu", columnDefinition = "TEXT", nullable = false)
    private String contenu;

    /** Rang dans le fil, à partir de 1 : l'ordre d'un échange ne se déduit pas d'un horodatage. */
    @Column(name = "rang", nullable = false)
    private int rang;

    /** Modèle ayant produit la réponse ; nul sur un message d'utilisateur. */
    @Column(name = "modele")
    private String modele;

    /** Version du prompt système en vigueur au moment de la réponse ; nul côté utilisateur. */
    @Column(name = "prompt_version")
    private String promptVersion;

    /** Durée de l'appel au modèle, en millisecondes ; nul côté utilisateur. */
    @Column(name = "duree_ms")
    private Long dureeMs;

    /**
     * Jetons consommés par l'appel qui a produit ce message. Portés par la réponse seule, et
     * décomptés du même budget quotidien que les suggestions : sans cela, la conversation
     * échapperait au plafond qui protège le forfait.
     */
    @Column(name = "jetons_utilises")
    private Long jetonsUtilises;
}
