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
public class ExigenceDto extends AuditEntityDto {

    private String libelleExigence;
    private String descriptionExigence;
    private String dateEcheanceExigence;
    private String statutConformite;
    private List<AuditeDto> audites;
    private List<ActionCorrectivePreventiveDto> actionCorrectivePreventives;
}
