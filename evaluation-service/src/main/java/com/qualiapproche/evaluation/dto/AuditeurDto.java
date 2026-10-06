package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.evaluation.enumeration.StatutAuditeur;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Un auditeur ; son nom et son courriel se lisent dans Keycloak par {@code utilisateurId}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class AuditeurDto {
    private UUID id;
    @NotBlank
    private String utilisateurId;
    private List<String> domainesHabilites;
    private List<String> certifications;
    private String disponibilite;
    private UUID structureAppartenance;
    private StatutAuditeur statut;
    private String niveauHabilitation;
    private List<String> processusGeres;
    private Double scoreEvaluationMoyen;
    /** Audits clôturés au cours des douze derniers mois, comme responsable ou membre. */
    private Long auditsRealises12Mois;
}
