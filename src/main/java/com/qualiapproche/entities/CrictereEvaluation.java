package com.qualiapproche.entities;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
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
public class CrictereEvaluation extends AuditEntity {

    private String noteAtribuerCritere;
    private String qualite;
    private String delaisLivraison;
    private String ServiceClient;
    @ManyToMany
    private List<Evaluation> evaluations;
}
