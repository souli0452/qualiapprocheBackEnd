package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.entities.Exigence;
import com.qualiapproche.entities.Reclamation;
import com.qualiapproche.entities.Risque;
import com.qualiapproche.utils.StatutEnum;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
public class ActionCorrectivePreventiveDto extends AuditEntityDto {

    private String libelle;
    private String description;
    private String responsable;
    private StatutEnum statut;
    private String type;
    private String dateDebut;
    private String dateFin;
    private Reclamation reclamation;
    private List<Risque> risques;
    private List<Exigence> exigences;
}
