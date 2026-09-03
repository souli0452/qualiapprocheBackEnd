package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Une ligne du décompte des non-conformités par service d'origine sur une "
        + "période. Projection d'une requête groupée : un service sans dossier n'a pas de ligne.")
public interface NonConformiteByStructDto {
    @Schema(description = "Sigle du service d'origine, tel qu'il était au moment de la "
            + "déclaration. Le regroupement se fait sur ce sigle et non sur l'identifiant du "
            + "service.",
            example = "MC")
    String getOrigineServiceLibelleCourt();
    @Schema(description = "Nombre de dossiers dont ce service est à l'origine sur la période "
            + "interrogée.",
            example = "9")
    Long getCount();
}
