package com.qualiapproche.dto;

import com.qualiapproche.entities.*;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

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
    private String responsable;
    private String mail;
    private String numeroTelephone;
    private String dateEcheance;
    private UUID nonConformiteID;
}
