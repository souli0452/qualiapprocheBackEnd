package com.qualiapproche.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SuiviAuditInspection extends AuditEntity {

    private String resultatSuiviAuditInspection;
    private String actionRecommender;
    private String statutSuiviAuditInspection;
    @ManyToMany
    private List<Reglementation> reglementations;


}
