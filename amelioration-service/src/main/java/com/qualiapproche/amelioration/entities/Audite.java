package com.qualiapproche.amelioration.entities;
import com.qualiapproche.common.base.AuditEntity;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Audite extends AuditEntity {

    private String libelleAudite;
    private String descriptionAudite;
    private String resultatAudite;
    private String statutAudite;
    private String objectifAudite;
    private String typeAudite;
    private UUID founisseurId;
  /*  @ManyToMany
    private List<Produit> produits;
    @OneToMany
    private List<Risque> risques;
    @OneToMany
    private List<NonConformite> nonConformites;
    @ManyToMany
    private List<Exigence> exigences;
    @ManyToMany
    private List<Departement> departements;*/

}
