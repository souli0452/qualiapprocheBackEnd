package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ContratAccord extends AuditEntity {
    private String libelleContratAccord;
    private String descriptionContratAccord;
    private String typeContratAccord;
    private LocalDateTime dateDebutContratAccord;
    private LocalDateTime dateFinContratAccord;
    private String termeConditionContratAccord;
    private String niveauService;
    private String conditionPaiement;
    private UUID fournisseurId;


}
