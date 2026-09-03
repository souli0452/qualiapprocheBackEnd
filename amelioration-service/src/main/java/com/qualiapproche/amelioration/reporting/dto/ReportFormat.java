package com.qualiapproche.amelioration.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Format de sortie de l'état. Quatre d'entre eux sont réellement produits — "
        + "PDF, WORD, EXCEL, CSV ; XPRINT n'est traité par aucun exportateur et rend un état vide "
        + "au lieu d'une erreur.")
public enum ReportFormat {
    PDF, WORD, EXCEL, CSV, XPRINT
}
