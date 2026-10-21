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
public class PointControleDto {
    private UUID id;
    private UUID checklistId;
    private String chapitreISO;
    @NotBlank
    private String libelle;
    private String question;
    private String preuveAttendue;
    private UUID auditeurAssigneId;
    /** Dans « mes points » : le constat que l'appelant a relevé sur ce point, s'il l'a fait. */
    private UUID constatId;
    /** Son palier ; nul tant que le point est « À saisir ». */
    private com.qualiapproche.evaluation.enumeration.StatutConstat statutConstat;
}
