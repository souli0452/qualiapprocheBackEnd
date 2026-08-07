package com.qualiapproche.referentiel.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Réglage de l'organisation, désigné par une clé : contact, téléphone, logo, adresse…
 *
 * <p>Ces valeurs n'ont pas de table propre à leur nature — un téléphone n'est pas un référentiel —
 * et se multiplieraient en autant de colonnes ou d'écrans. Une clé, une valeur : l'organisation en
 * ajoute sans qu'on livre du code, et les services les lisent sans les connaître à l'avance. Le pied
 * de page des courriels en est le premier usage.</p>
 *
 * <p><b>La clé ne se modifie pas</b> ({@code updatable = false}, et le service refuse la tentative
 * en 409). C'est par elle que le code désigne un réglage : la renommer romprait silencieusement tout
 * ce qui la lit, et l'ancien nom ne rendrait plus rien. Un réglage mal nommé se supprime et se
 * recrée — geste explicite, aux conséquences visibles.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@Table(name = "parametres",
        uniqueConstraints = @UniqueConstraint(name = "uk_parametre_cle", columnNames = "cle"))
public class Parametre extends AuditEntity {

    /**
     * Identité du réglage, en majuscules et soulignés : {@code CONTACT_EMAIL}, {@code LOGO_URL}.
     *
     * <p>Normalisée à la création — la casse et les espaces ne traduisent aucune intention distincte,
     * alors qu'ils feraient de {@code contact email} et {@code CONTACT_EMAIL} deux réglages
     * concurrents dont un seul serait lu.</p>
     */
    @Column(name = "cle", nullable = false, updatable = false, length = 80)
    private String cle;

    /** Ce que le réglage vaut. Vide, il est simplement ignoré par ce qui l'utilise. */
    @Column(name = "valeur", length = 2000)
    private String valeur;

    /** Intitulé lisible, pour l'écran d'administration. */
    @Column(nullable = false)
    private String libelle;

    /** À quoi ce réglage sert, et où il apparaît. */
    private String description;

    /**
     * Nature de la valeur, pour que l'écran sache la présenter et la vérifier.
     *
     * <p>Un logo se saisit comme une adresse d'image et s'affiche comme telle ; un téléphone se
     * compose. Sans cette indication, tout devient une chaîne libre et l'écran ne peut ni aider ni
     * prévenir.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeParametre type;

    /**
     * Le réglage peut-il être lu sans habilitation particulière ?
     *
     * <p>Vrai pour ce qui figure déjà sur un courriel ou une page publique — contact, téléphone,
     * logo : le service qui compose un pied de page le fait souvent hors de toute requête
     * utilisateur, sans permission à présenter. Faux par défaut : un réglage n'est pas public parce
     * qu'on a oublié d'y penser.</p>
     */
    @Column(name = "lisible_sans_habilitation", nullable = false)
    @lombok.Builder.Default
    private boolean lisibleSansHabilitation = false;
}
