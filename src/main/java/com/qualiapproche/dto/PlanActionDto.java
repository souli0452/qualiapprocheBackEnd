package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.entities.*;
import com.qualiapproche.utils.StatutEnum;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.time.LocalDate;
import java.util.UUID;

import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder

public class PlanActionDto extends AuditEntityDto {
    private String numeroOdre;
    private String causeIdentifiees;
    private String solutionRetenues;
    private String responsableId;
    private String responsableNomComplet;
    @Enumerated(EnumType.STRING)
    private StatutEnum status;
    private String responsableEmail;
    private String numeroTelephone;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateEcheance;
    private UUID nonConformiteID;
    private String numeroNc;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateTraitement;
    private String procEmetteur;
    private  NonConformiteDto nonConformite;
}
