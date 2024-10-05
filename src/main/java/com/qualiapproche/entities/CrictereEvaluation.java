package com.qualiapproche.entities;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class CrictereEvaluation extends AuditEntity {

    private String libelleCrictereEvaluation;
    private String descriptionCrictereEvaluation;
    private String noteAtribuerCritere;
    private String delaisLivraison;
    private String ServiceClient;
    private String commentaireEvaluation;
    @ManyToOne
    @JoinColumn(name = "fournisseur_id")
   // @JsonBackReference
    private Fournisseur fournisseur;
}
