package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Exigence extends AuditEntity {

    private String libelleExigence;
    private String descriptionExigence;
    private String dateEcheanceExigence;
    private String statutConformite;
    @ManyToMany
    private List<Audite> audites;
    @ManyToMany
    private List<Reglementation> reglementations;
    @ManyToMany
    private List<Formation> formations;
    @ManyToMany
    private List<ActionCorrectivePreventive> actionCorrectivePreventives;


}
