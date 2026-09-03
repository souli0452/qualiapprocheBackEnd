package com.qualiapproche.common.dto;



import com.qualiapproche.common.enumeration.Status;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Une ligne du décompte des non-conformités d'une structure : un état, et le "
        + "nombre de dossiers qui le portent. Projection d'une requête groupée : un état sans "
        + "dossier n'a pas de ligne, il n'apparaît pas à zéro.")
public interface NcStats {
    @Schema(description = "État sous lequel les dossiers de la ligne sont rangés.",
            example = "IN_PROGRESS")
    Status getStatus();
    @Schema(description = "Nombre de dossiers de la structure interrogée portant cet état.",
            example = "12")
    Integer getCount();
}
