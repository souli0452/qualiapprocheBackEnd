package com.qualiapproche.amelioration.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author : <a href="siguizana08@gmail.com">BRAHIMA TRAORE </a>.
 * @version : 1.0
 * @since : 08/03/2022 à 12:14:46
 */
@Schema(description = "État demandé. Chaque valeur désigne un gabarit Jasper au catalogue des "
        + "modèles ; une valeur sans gabarit déclaré est refusée à la génération, et non à la "
        + "lecture de la requête.")
public enum EReportType {

    NON_CONFORMITE,

}
