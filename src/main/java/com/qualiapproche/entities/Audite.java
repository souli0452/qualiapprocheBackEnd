package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Audite extends AuditEntity {

    private String libelleAudite;
    private String descriptionAudite;
    private String resultatAudite;
    private String statutAudite;
    private String objectifAudite;
    private String typeAudite;
    private UUID FounisseurId;
    @ManyToMany
    private List<Produit> produits;
    @OneToMany
    private List<Risque> risques;
    @OneToMany
    private List<NonConformite> nonConformites;
    @ManyToMany
    private List<Exigence> exigences;
    @ManyToMany
    private List<Departement> departements;

}
