package com.qualiapproche.common.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class SuiviAuditInspectionDto extends AuditEntityDto {
    private String resultatSuiviAuditInspection;
    private String actionRecommender;
    private String statutSuiviAuditInspection;
    private List<ReglementationDto> reglementations;
}
