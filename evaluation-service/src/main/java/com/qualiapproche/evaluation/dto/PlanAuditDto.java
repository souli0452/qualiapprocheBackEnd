package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.evaluation.enumeration.OrigineDocument;
import com.qualiapproche.evaluation.enumeration.StatutPlanAudit;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
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
public class PlanAuditDto {
    private UUID id;
    private UUID auditId;
    private String reference;
    private String objectifs;
    private String portee;
    private String criteres;
    private List<String> methodesAudit;
    private String lieu;
    private String langue;
    private String moyensNecessaires;
    private OrigineDocument origine;
    private boolean fichierCharge;
    private String nomFichierCharge;
    private StatutPlanAudit statut;
    private LocalDateTime datePartage;
    @Valid
    private List<ActivitePlanDto> activites;
}
