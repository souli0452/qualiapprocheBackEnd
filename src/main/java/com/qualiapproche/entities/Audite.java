package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Audite extends AuditEntity {

    private String libelleAudite;
    private String descriptionAudite;
    private String resultatAudite;
    private String statutAudite;
    private String objectifAudite;
    private String typeAudite;
    @ManyToMany
    private List<Produit> produits;
    @OneToMany
    private List<Risque> risques;
    @OneToMany
    private List<NonConformite> nonConformites;
    @ManyToMany
    private List<Fournisseur> fournisseurs;
    @ManyToMany
    private List<Exigence> exigences;
    @ManyToMany
    private List<Departement> departements;

}
