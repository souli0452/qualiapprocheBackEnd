package com.qualiapproche.referentiel.entities;
import com.qualiapproche.common.base.AuditEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class SuiviAuditInspection extends AuditEntity {

    private String resultatSuiviAuditInspection;
    private String actionRecommender;
    private String statutSuiviAuditInspection;
    @ManyToMany
    private List<Reglementation> reglementations;


}
