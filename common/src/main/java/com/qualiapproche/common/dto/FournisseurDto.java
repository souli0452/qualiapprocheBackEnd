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
public class FournisseurDto extends AuditEntityDto{
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private String siteWeb;
    private String contactPrincipal;
    private String statut;
    
    private List<CrictereEvaluationDto> criteresEvaluation;

}
