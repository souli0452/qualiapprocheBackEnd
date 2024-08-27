package com.qualiapproche.dto;

import com.qualiapproche.entities.ActionCorrectivePreventive;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

public class RisqueDto extends  AuditEntityDto {
    private String libelleRisque;
    private String descriptionRisque;
    private String niveauRisque;
    private String statutRisque;
    private String plantAttenuation;
    private String commentaireRisque;
    private String evidenceRisuqe;
    private LocalDateTime dateIdentificationRisque;
    private List<ActionCorrectivePreventive> actionCorrectivePreventives;
}
