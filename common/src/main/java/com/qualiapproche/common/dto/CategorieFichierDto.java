package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;










@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder

@Schema(description = "Famille sous laquelle les fichiers du système documentaire sont rangés : "
        + "procédures, enregistrements, formulaires. La nomenclature appartient à l'organisation.")
public class CategorieFichierDto extends AuditEntityDto {

    @Schema(description = "Intitulé de la catégorie, tel qu'il apparaît dans les listes de choix.",
            example = "Procédures")
    private String libelleCategorie;

    @Schema(description = "Ce que la catégorie recueille : ce qui fait qu'un fichier y a sa place "
            + "plutôt que dans la voisine.",
            example = "Documents décrivant la manière de conduire une activité maîtrisée.")
    private String descriptionCategorie;

    @Schema(description = "Marque les catégories dont un fichier ne se dépose qu'après demande "
            + "préalable. Champ libre, laissé à la convention de l'écran : aucun traitement du "
            + "serveur ne s'y fie, et il ne bloque donc rien de lui-même.",
            example = "OUI")
    private String necessiteDemandeCreationFichier;
    //private List<FichierDto> fichiers;
}
