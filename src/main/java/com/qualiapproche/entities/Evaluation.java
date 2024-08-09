package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Evaluation extends AuditEntity {

    private String libelle;
    private String description;
    private String typeEvaluation;

    @OneToMany
    private List<Formation> formations;


    private LocalDateTime dateEvaluation;
    @ManyToMany
    private List<Fournisseur> fournisseurs;
    @ManyToMany
    private List<ActionCorrectivePreventive> actionCorrectivePreventiveReconmenders;
}
