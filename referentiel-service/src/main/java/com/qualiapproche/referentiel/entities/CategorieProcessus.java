package com.qualiapproche.referentiel.entities;

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
@Table(name = "categorie_processus")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Schema(
    description = "Catégorie ou macro-famille de processus selon la norme ISO 9001. "
        + "Permet à l'IA et aux tableaux de bord de classer les dysfonctionnements selon les 3 grands piliers : "
        + "1. Direction (Management/Stratégie), 2. Réalisation (Cœur de métier/Opérationnel), 3. Support (Ressources/Soutien)."
)
public class CategorieProcessus extends AuditEntity {

    /**
     * Intitulé officiel de la catégorie.
     * Exemples : "Direction", "Réalisation", "Support".
     */
    @Schema(
        description = "Intitulé de la macro-famille de processus (ex: Direction, Réalisation, Support)",
        example = "Direction",
        allowableValues = {"Direction", "Réalisation", "Support"}
    )
    @Column(name = "libelle", nullable = false)
    private String libelle;

    /**
     * Définition détaillée du rôle de cette catégorie dans l'organisme.
     * Exemple pour Direction : Processus qui déterminent la stratégie, les objectifs et la politique qualité.
     */
    @Schema(
        description = "Définition et périmètre d'action de cette catégorie dans la cartographie des processus de l'entreprise",
        example = "Processus contribuant à la définition, au pilotage de la politique qualité et au déploiement des objectifs stratégiques."
    )
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
