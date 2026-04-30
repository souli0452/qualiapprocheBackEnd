package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 




import java.util.List;




@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class ReglementationDto extends AuditEntityDto{

    private String nomReglementation;
    private String descriptionReglementation;
    private String organismeReglementation;
    
    private List<ExigenceDto> exigences;
    
    private List<SuiviAuditInspectionDto> suiviAuditInspections;
}
