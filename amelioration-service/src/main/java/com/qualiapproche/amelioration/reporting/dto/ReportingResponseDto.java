package com.qualiapproche.amelioration.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;

/**
 * @author : <a href="siguizana08@gmail.com">BRAHIMA TRAORE </a>.
 * @version : 1.0
 * @since : 08/03/2022 à 13:10:05
 */
@NoArgsConstructor
@SuppressWarnings("ALL")
@Schema(description = "État produit, porté en mémoire. Enveloppe interne : le point d'entrée "
        + "d'édition n'en rend que le contenu binaire, jamais cet objet.")
public class ReportingResponseDto {

    @Schema(description = "Contenu du fichier produit, dans le format demandé. Nul lorsque "
            + "l'édition a échoué : l'erreur est journalisée et l'appelant reçoit un état vide "
            + "plutôt qu'un refus.")
    private byte[] reportFile;

    /**
     * ArgsConstructor.
     *
     * @param reportFile
     */
    public ReportingResponseDto(final byte[] reportFile) {
        if (reportFile != null) {
            this.reportFile = reportFile.clone();
        } else {
            this.reportFile = null;
        }
    }

    /**
     * Getter for reportFile.
     *
     * @return byte[]
     */
    public byte[] getReportFile() {
        return reportFile != null ? reportFile.clone() : null;
    }
}
