package com.qualiapproche.referentiel.entities;
import com.qualiapproche.common.base.AuditEntity;


import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
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
