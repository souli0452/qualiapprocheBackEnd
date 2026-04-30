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
public class ExigenceDto extends AuditEntityDto{

    private String libelleExigence;
    private String descriptionExigence;
    private String dateEcheanceExigence;
    private String statutConformite;
    private List<AuditeDto> audites;
    private List<ActionCorrectivePreventiveDto> actionCorrectivePreventives;
}
