package com.qualiapproche.common.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

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
}
