package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;







import java.util.UUID;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class CrictereEvaluationDto extends AuditEntityDto {

    private String libelleCrictereEvaluation;
    private String descriptionCrictereEvaluation;
    private String noteAtribuerCritere;
    private String delaisLivraison;
    private String serviceClient;
    private String commentaireEvaluation;


   // @JsonBackReference
    private UUID fournisseurId;


}
