package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NonConformite extends AuditEntity {
    private String intitule;
    private String typeNonConformite;
    private String numeroReference;
    private String priorite;
    private String detailleSuplementaire;
    private LocalDateTime dateEcheance;
    private String statut;
    private String commentaires;
    @OneToOne
    private Reclamation reclamation;
    @OneToMany
    private List<Fichier> fichiers;
    @ManyToMany
    private List<Audite> audites;

}
