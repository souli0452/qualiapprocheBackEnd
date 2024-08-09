package com.qualiapproche.dto;

import com.qualiapproche.entities.Fournisseur;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ContratAccordDto extends AuditEntityDto{

    private String libelleContratAccord;
    private String descriptionContratAccord;
    private String typeContratAccord;
    private LocalDateTime dateDebutContratAccord;
    private LocalDateTime dateFinContratAccord;
    private String termeConditionContratAccord;
    private String niveauService;
    private String conditionPaiement;
    private Fournisseur fournisseur;
}
