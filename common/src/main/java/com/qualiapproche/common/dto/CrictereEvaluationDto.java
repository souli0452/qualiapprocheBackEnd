package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 
import com.fasterxml.jackson.annotation.JsonBackReference;




import java.util.List;
import java.util.UUID;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class CrictereEvaluationDto extends AuditEntityDto{

    private String libelleCrictereEvaluation;
    private String descriptionCrictereEvaluation;
    private String noteAtribuerCritere;
    private String delaisLivraison;
    private String serviceClient;
    private String commentaireEvaluation;

    
   // @JsonBackReference
    private UUID fournisseurId;


}
