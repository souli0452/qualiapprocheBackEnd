package com.qualiapproche.reporting.dto;

import lombok.NoArgsConstructor;

/**
 * @author : <a href="siguizana08@gmail.com">BRAHIMA TRAORE </a>.
 * @version : 1.0
 * @since : 08/03/2022 à 13:10:05
 */
@NoArgsConstructor
@SuppressWarnings("ALL")
public class ReportingResponseDto {
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
