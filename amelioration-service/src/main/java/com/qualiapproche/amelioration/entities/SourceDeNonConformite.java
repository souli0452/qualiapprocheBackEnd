package com.qualiapproche.amelioration.entities;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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

@Entity
@Table(name = "source_de_non_conformite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class SourceDeNonConformite extends AuditEntity {

    /**
     * Libellé de la source d'une non-conformité.
     * Exemple : Audit interne, Réclamation client...
     */
    @JsonPropertyDescription("Source d'une non-conformité (Exemple : Audit interne)")
    @Column(name = "libelle_source_non_conformite", nullable = false)
    private String libelle;

    /**
     * Description détaillée de la source d'une non-conformité.
     * Exemple : L'anomalie est levée par un collaborateur désigné pour vérifier...
     */
    @JsonPropertyDescription("Description de la source d'une non-conformité")
    @Column(name = "description_source_non_conformite", columnDefinition = "TEXT")
    private String description;
}
