package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 





import java.time.LocalDateTime;
import java.util.List;





@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class RisqueDto extends  AuditEntityDto {
    private String libelle;
    private String description;
    private String niveau;
    private String statut;
    private String plantAttenuation;
    private String commentaireRisque;
    private String evidenceRisque;
    // private LocalDateTime dateIdentificationRisque;
    private List<ActionCorrectivePreventiveDto> actionCorrectivePreventives;


}
