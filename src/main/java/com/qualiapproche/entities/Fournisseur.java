package com.qualiapproche.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.*;
import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Fournisseur extends AuditEntity {
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private String siteWeb;
    private String contactPrincipal;
    private String statut;
    @ManyToMany
    private List<Audite> audites;
    @OneToMany
    private List<ContratAccord> contratAccords;
    @ManyToMany
    private List<Evaluation> evaluations;

}
