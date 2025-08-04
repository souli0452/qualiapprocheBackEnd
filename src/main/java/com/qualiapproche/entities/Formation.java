package com.qualiapproche.entities;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.utils.StatutEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
public class Formation extends AuditEntity {

    private String libelle;
    private String description;
    private String objectif;
    private String prerequis;
    private String competence;
    private StatutEnum statut;
    @ManyToMany
    private List<Exigence> exigences;
    @OneToOne
    private Evaluation evaluation;
}
