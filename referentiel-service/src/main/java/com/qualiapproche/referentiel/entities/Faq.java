package com.qualiapproche.referentiel.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Une entrée de la foire aux questions de l'application.
 *
 * <p>Un référentiel comme les autres : l'organisation y écrit ses réponses, ses utilisateurs les
 * consultent depuis l'aide. L'assistant IA en est un lecteur supplémentaire — il la reçoit dans
 * sa consigne et y puise quand une question s'en approche, au lieu d'inventer. Mais c'est un
 * usage de la FAQ, pas sa raison d'être : elle doit valoir pour une installation qui ne
 * souscrira jamais au module ASSISTANT_IA.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@Table(name = "faq")
public class Faq extends AuditEntity {

    @Column(name = "question", columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(name = "reponse", columnDefinition = "TEXT", nullable = false)
    private String reponse;

    /** Regroupement dans l'aide. Libre : les rubriques d'une organisation ne se devinent pas. */
    @Column(name = "categorie", length = 120)
    private String categorie;

    /**
     * Visible dans l'aide, et récitée par l'assistant.
     *
     * <p>Dépublier n'efface ni le texte ni son auteur : une réponse suspendue le temps d'une
     * revue se retire d'un geste et revient du même.</p>
     */
    @Column(name = "publiee", nullable = false)
    private boolean publiee;

    /**
     * Ordre d'affichage.
     *
     * <p>Il compte au-delà de l'esthétique : la consigne de l'assistant porte un plafond de
     * caractères, et ce qui le dépasse ne lui est pas envoyé. Ce qui importe le plus se place
     * donc en tête.</p>
     */
    @Column(name = "rang", nullable = false)
    private int rang;
}
