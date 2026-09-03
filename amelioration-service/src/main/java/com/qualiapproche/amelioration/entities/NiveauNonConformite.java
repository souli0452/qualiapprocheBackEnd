package com.qualiapproche.amelioration.entities;
import com.qualiapproche.common.base.AuditEntity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Gravité d'une non-conformité, paramétrée par l'organisation. */
@Getter
@Setter
@Entity
@Table(name = "niveau_non_conformite")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class NiveauNonConformite extends AuditEntity {

    private String libelle;

    private String description;

    /**
     * Poids du niveau, du moins grave au plus grave.
     *
     * <p>C'est lui qui permet de trier et de comparer deux niveaux. Le libellé ne le permet pas :
     * « Majeure » et « Mineure » se suivent dans l'ordre alphabétique en sens inverse de leur
     * gravité. L'échelle appartient à l'organisation, qui décide de son étendue.</p>
     */
    private Integer score;

    /** Couleur d'affichage du niveau (ex. {@code #f59e0b}), facultative. */
    @Column(length = 20)
    private String couleur;
}
