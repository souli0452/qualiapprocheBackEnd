package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;




import com.qualiapproche.common.utils.StatutEnum;



import java.util.List;




@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class FormationDto extends AuditEntityDto {

    private String libelle;
    private String description;
    private String objectif;
    private String prerequis;
    private String competence;
    private StatutEnum statut;
    private List<ExigenceDto> exigences;
    private EvaluationDto evaluation;
}
