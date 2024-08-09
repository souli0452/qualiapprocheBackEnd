package com.qualiapproche.dto;

import com.qualiapproche.entities.Fournisseur;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EvaluationDto extends AuditEntityDto{

    private String actionCorrectivePreventiveReconmender;
    private String commentaireEvaluation;
    private LocalDateTime dateEvaluation;
    private List<Fournisseur> fournisseurs;
}
