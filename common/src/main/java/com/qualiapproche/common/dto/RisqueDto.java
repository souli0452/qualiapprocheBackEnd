package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;








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
