package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class NonConformite extends AuditEntity {
    private String intitule;
    private String typeNonConformite;
    private String numeroReference;
    private String priorite;
    private String detailleSuplementaire;
    private String dateEcheance;
    private String statut;
    private String commentaires;
    @OneToOne
    private Reclamation reclamation;
    @OneToMany
    private List<Fichier> fichiers;
    @ManyToMany
    private List<Audite> audites;

}
