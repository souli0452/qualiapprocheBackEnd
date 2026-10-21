package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class EvaluationAuditeurDto {
    private UUID id;
    @NotNull
    private UUID auditId;
    @NotNull
    private UUID auditeurId;
    @NotNull
    private UUID critereId;
    private String critereLibelle;
    @NotNull
    private UUID niveauId;
    private String niveauLibelle;
    private LocalDate dateEvaluation;
    private String commentaire;
    private StatutEvaluation statut;
}
