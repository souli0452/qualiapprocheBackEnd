package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class CrictereEvaluation extends AuditEntity {

    private String noteAtribuerCritere;
    private String qualite;
    private String delaisLivraison;
    private String ServiceClient;
    @ManyToMany
    private List<Evaluation> evaluations;
}
