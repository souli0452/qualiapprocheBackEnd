package com.qualiapproche.common.dto;

import com.qualiapproche.common.enumeration.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Les chiffres d'un tableau de bord de non-conformités. Le périmètre — tout "
        + "l'organisme, une structure, un agent — est fixé par le point d'entrée appelé et n'est "
        + "pas rappelé ici.")
public class NcDashboardDto {
    @Schema(description = "Nombre de dossiers du périmètre, compté avant toute répartition. Les "
            + "deux répartitions écartent les dossiers dont l'état ou la gravité manque : leur "
            + "somme peut donc lui être inférieure.",
            example = "148")
    private long totalNC;

    @Schema(description = "Nombre de dossiers par état. Un état sans dossier est absent de la "
            + "carte plutôt que présent à zéro : c'est au client d'afficher le zéro.")
    private Map<Status, Long> statsByStatus;

    @Schema(description = "Même répartition, ventilée par libellé de niveau de gravité au sein de "
            + "chaque état. Un dossier dont le niveau n'est pas renseigné n'y figure pas.")
    private Map<Status, Map<String, Long>> statsByStatusAndGravity;
}
