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
