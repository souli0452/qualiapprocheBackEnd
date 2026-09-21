package com.qualiapproche.ia.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.ia.enumeration.TypeAssistance;
import com.qualiapproche.ia.enumeration.VerdictSuggestion;
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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Trace d'une suggestion de l'assistant.
 *
 * <p>Exigence de traçabilité du SGQ : chaque texte proposé par l'IA laisse un enregistrement
 * (prompt utilisé et sa version, modèle, matière fournie, texte produit, sort réservé par
 * l'utilisateur). C'est ce qui permet, lors d'un audit, de répondre à « d'où vient ce texte et
 * qui l'a validé ».</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@Table(name = "suggestions_ia")
public class SuggestionIa extends AuditEntity {

    /** Nature de l'assistance ayant produit la suggestion. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_assistance", nullable = false)
    private TypeAssistance typeAssistance;

    /** Version du prompt système utilisé, telle que portée par {@code PromptRegistry}. */
    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    /** Modèle ayant produit la suggestion (ex. mistral-small-latest). */
    @Column(name = "modele")
    private String modele;

    /** Brouillon ou notes fournis par le demandeur. */
    @Column(name = "texte_source", columnDefinition = "TEXT")
    private String texteSource;

    /** Éléments de contexte fournis, sérialisés en JSON. */
    @Column(name = "contexte", columnDefinition = "TEXT")
    private String contexte;

    /** Texte produit par l'assistant. */
    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    /** Sort réservé par l'utilisateur ; nul tant qu'il ne s'est pas prononcé. */
    @Enumerated(EnumType.STRING)
    @Column(name = "verdict")
    private VerdictSuggestion verdict;

    /** Type de la ressource métier concernée (facultatif). */
    @Column(name = "ressource_type")
    private String ressourceType;

    /** Identifiant de la ressource métier concernée (facultatif). */
    @Column(name = "ressource_id")
    private UUID ressourceId;

    /** Durée de l'appel au modèle, en millisecondes. */
    @Column(name = "duree_ms")
    private Long dureeMs;

    /** Jetons consommés par l'appel, si le fournisseur les rapporte. */
    @Column(name = "jetons_utilises")
    private Long jetonsUtilises;
}
