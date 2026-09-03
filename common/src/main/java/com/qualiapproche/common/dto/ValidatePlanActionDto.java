package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "La validation en un seul geste de plusieurs plans d'action d'une même "
        + "non-conformité. Aucun point d'entrée ne le reçoit aujourd'hui.")
public class ValidatePlanActionDto {
    @Schema(description = "Non-conformité dont les plans sont visés. Elle borne la liste : un "
            + "plan rattaché à un autre dossier n'a rien à y faire.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private UUID nonConformiteId;

    @Schema(description = "Plans d'action à valider. Ceux qui n'y figurent pas sont laissés en "
            + "l'état : la liste vaut choix, non énumération de tout ce que porte le dossier.")
    private List<UUID> planIds;
}
