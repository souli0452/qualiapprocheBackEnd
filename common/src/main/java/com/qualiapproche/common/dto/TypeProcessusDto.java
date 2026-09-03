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

@Schema(description = "Famille à laquelle un processus se rattache dans la cartographie : "
        + "pilotage, réalisation, support. Elle classe les processus, elle ne les nomme pas.")
public class TypeProcessusDto extends AuditEntityDto {

    @Schema(description = "Intitulé de la famille, tel qu'il apparaît dans les listes de choix.",
            example = "Réalisation")
    private String libelle;

    @Schema(description = "Ce que la famille recouvre : le rôle commun aux processus qui s'y "
            + "rangent.",
            example = "Processus qui concourent directement à la fourniture du service au client.")
    private String description;
}
