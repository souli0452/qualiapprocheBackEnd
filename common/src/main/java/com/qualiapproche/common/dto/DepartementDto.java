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

@Schema(description = "Département de l'organisation, tenu au référentiel. À ne pas confondre "
        + "avec la structure : celle-ci porte les dossiers et commande qui les voit, le "
        + "département ne fait que nommer un découpage.")
public class DepartementDto extends AuditEntityDto {

    @Schema(description = "Intitulé du département, tel qu'il apparaît dans les listes de choix.",
            example = "Département des achats")
    private String libelleDepartement;

    @Schema(description = "Ce dont le département a la charge, pour distinguer deux intitulés "
            + "voisins.",
            example = "Achats de fournitures et de prestations courantes.")
    private String descriptionDepartement;

}
