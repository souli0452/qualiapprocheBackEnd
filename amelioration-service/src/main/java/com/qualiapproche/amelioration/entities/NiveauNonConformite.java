package com.qualiapproche.amelioration.entities;
import com.qualiapproche.common.base.AuditEntity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@Table(name = "niveau_non_conformite")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Schema(description = "Échelle de gravité ou de criticité d'une non-conformité (ex: Mineure, Majeure, Critique).")
public class NiveauNonConformite extends AuditEntity {

    @Schema(description = "Intitulé du niveau de gravité", example = "Majeure")
    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Schema(
        description = "Critères d'appréciation et définition du niveau de gravité selon la politique qualité",
        example = "Défaillance susceptible d'altérer la qualité du livrable ou non-respect direct d'une exigence contractuelle."
    )
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * (Optionnel) Poids numérique pour le tri et le calcul du niveau de risque par l'IA.
     * 1 = Mineure, 2 = Majeure, 3 = Critique.
     */
    @Schema(description = "Score numérique de sévérité pour les statistiques et matrices de risque (1=Faible, 2=Moyen, 3=Élevé)", example = "2")
    @Column(name = "score_gravite")
    private Integer scoreGravite;

    /**
     * (Optionnel) Code couleur hexadécimal pour l'affichage des badges dans l'interface.
     */
    @Schema(description = "Code couleur hexadécimal pour le badge UI", example = "#f59e0b")
    @Column(name = "couleur", length = 20)
    private String couleur;
}
