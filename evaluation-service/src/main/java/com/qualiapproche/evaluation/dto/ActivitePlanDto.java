package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
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
public class ActivitePlanDto {
    private UUID id;
    private String horaireDebut;
    private String horaireFin;
    @NotBlank
    private String libelleActivite;
    private String processusConcerne;
    private String auditeur;
    private String personnesAuditees;
}
