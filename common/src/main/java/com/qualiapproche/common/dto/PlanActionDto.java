package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.common.utils.StatutEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor

@SuperBuilder

public class PlanActionDto extends AuditEntityDto {
    private String numeroOdre;
    private String causeIdentifiees;
    private String solutionRetenues;
    private String responsableId;
    private String responsableNomComplet;

    private StatutEnum status;
    private String responsableEmail;
    private String numeroTelephone;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateEcheance;
    private String numeroNc;

    private LocalDate dateTraitement;
    private String procEmetteur;
    private NonConformiteDto nonConformite;
    private List<PieceJointeDTO> fichiers;
    private String observation;
    private UUID nonConformeId;
    private String observationRejet;
    private String actionCorrective;
    private PieceJointeDTO docRejet;

    private LocalDate dateRejet;
    private String critereEfficacite;
    /**
     * Ce que le responsable qualité a observé, confronté au critère d'efficacité fixé à la
     * définition de l'action. Recueilli par l'étape « Efficacité à mesurer » du circuit.
     */
    private String constatEfficacite;
    private UUID workflowId;
    private String workflowStatus;

    /** État du circuit de validation du plan d'action (cf. {@link NonConformiteDto#getWorkflowState()}). */
    private WorkflowStateDto workflowState;
}
