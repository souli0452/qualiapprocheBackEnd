package com.qualiapproche.dto;

import com.qualiapproche.entities.Reglementation;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

public class SuiviAuditInspectionDto extends  AuditEntityDto{
    private String resultatSuiviAuditInspection;
    private String actionRecommender;
    private String statutSuiviAuditInspection;
    private List<Reglementation> reglementations;

}
