package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 

import com.qualiapproche.common.utils.StatutEnum;



import java.util.List;




@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class FormationDto extends AuditEntityDto{

    private String libelle;
    private String description;
    private String objectif;
    private String prerequis;
    private String competence;
    private StatutEnum statut;
    private List<ExigenceDto> exigences;
    private EvaluationDto evaluation;
}
