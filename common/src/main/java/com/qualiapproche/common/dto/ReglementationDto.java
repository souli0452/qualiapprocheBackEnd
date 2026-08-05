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
public class ReglementationDto extends AuditEntityDto {

    private String nomReglementation;
    private String descriptionReglementation;
    private String organismeReglementation;

    private List<ExigenceDto> exigences;

    private List<SuiviAuditInspectionDto> suiviAuditInspections;
}
