package com.qualiapproche.amelioration.entities;
import com.qualiapproche.referentiel.entities.Formation;
import com.qualiapproche.common.base.AuditEntity;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Evaluation extends AuditEntity {

    private String libelle;
    private String description;
    private String typeEvaluation;

/*    @OneToMany
    private List<Formation> formations;*/

    private LocalDateTime dateEvaluation;
    private UUID fournisseurId;
    @ManyToMany
    private List<ActionCorrectivePreventive> actionCorrectivePreventiveReconmenders;
}
