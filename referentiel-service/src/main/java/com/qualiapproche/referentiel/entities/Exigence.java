package com.qualiapproche.referentiel.entities;
import com.qualiapproche.common.base.AuditEntity;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class Exigence extends AuditEntity {

    private String libelleExigence;
    private String descriptionExigence;
    private String dateEcheanceExigence;
    private String statut;
    /*
    @ManyToMany
    @JoinTable( name = "exigence_audite", joinColumns = @JoinColumn(name = "exigence_id"), inverseJoinColumns = @JoinColumn(name = "audite_id"))
    private List<Audite> audites;
    */
    @ManyToMany
    private List<Reglementation> reglementations;
    @ManyToMany
    private List<Formation> formations;

}
