package com.qualiapproche.dto;

import com.qualiapproche.entities.Evaluation;
import lombok.*;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CrictereEvaluationDto extends AuditEntityDto{

    private String noteAtribuerCritere;
    private String qualite;
    private String delaisLivraison;
    private String ServiceClient;
    private List<Evaluation> evaluations;

}
