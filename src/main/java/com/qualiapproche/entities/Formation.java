package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.util.List;

@Entity
@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Formation extends AuditEntity {

    private String libelleFormation;
    private String descriptionFormation;
    private String objectifFormation;
    private String prerequisFormation;
    private String compétenceAcquise;
    @ManyToMany
    private List<Exigence> exigences;
    @OneToOne
    private Evaluation evaluationFormation;
}
