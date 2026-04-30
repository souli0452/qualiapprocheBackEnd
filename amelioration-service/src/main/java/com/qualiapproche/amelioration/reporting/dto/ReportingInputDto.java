package com.qualiapproche.amelioration.reporting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @author : <a href="siguizana08@gmail.com">BRAHIMA TRAORE </a>.
 * @version : 1.0
 * @since : 08/03/2022 à 13:12:54
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportingInputDto {
    private ReportFormat reportFormat = ReportFormat.PDF;
    @NotNull
    private EReportType reportType;
    private UUID entityId;
}
