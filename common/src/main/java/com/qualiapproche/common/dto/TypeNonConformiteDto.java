package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;









@Data
@AllArgsConstructor
@NoArgsConstructor
//

@SuperBuilder

@Schema(description = "Origine d'un constat de non-conformité : ce qui l'a fait apparaître — "
        + "audit interne, réclamation client, contrôle de réception. À distinguer du niveau, qui "
        + "en dit la gravité.")
public class TypeNonConformiteDto extends AuditEntityDto {

    @Schema(description = "Intitulé de l'origine, tel qu'il apparaît dans les listes de choix.",
            example = "Audit interne")
    private String libelle;

    @Schema(description = "Ce qui range un constat sous cette origine plutôt qu'une autre, afin "
            + "que deux structures déclarent de la même façon.",
            example = "Écart relevé au cours d'un audit interne programmé.")
    private String description;
}
