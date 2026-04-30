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



import java.time.LocalDateTime;
import java.util.List;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class ActionCorrectivePreventiveDto extends AuditEntityDto {

    private String libelle;
    private String description;
    private String responsable;
    private StatutEnum statut;
    private String type;
    private String dateDebut;
    private String dateFin;
    private ReclamationDto reclamation;
    private List<RisqueDto> risques;
    private List<ExigenceDto> exigences;
}
