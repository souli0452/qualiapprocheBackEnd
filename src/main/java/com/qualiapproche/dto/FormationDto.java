package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.entities.Evaluation;
import com.qualiapproche.entities.Exigence;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
public class FormationDto extends AuditEntityDto{

    private String libelle;
    private String description;
    private String objectif;
    private String prerequis;
    private String competence;
    private List<Exigence> exigences;
    private Evaluation evaluation;
}
