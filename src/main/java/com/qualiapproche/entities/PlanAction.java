/*
package com.qualiapproche.entities;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class PlanAction extends AuditEntity{

    private String numeroOdre;
    private String causeIdentifiees;
    private String solutionRetenues;
    private String responsable;
    private String mail;
    private String numeroTelephone;
    private String dateEcheance;
    @ManyToOne
    @JoinColumn(name = "non_conformite_id", nullable = false)
    private NonConformite nonConformite;
}
*/

package com.qualiapproche.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.qualiapproche.utils.StatutEnum;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class PlanAction extends AuditEntity {

    private String numeroOdre;
    private  String observation;
    private String causeIdentifiees;
    private String solutionRetenues;
    private String responsableNomComplet;
    private String responsableId;
    private String responsableEmail;
    private String numeroTelephone;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateEcheance;
    @OneToMany
    private List<Fichier> fichiers;
    @Enumerated(EnumType.STRING)
    private StatutEnum status;
    private String numeroNc;
    private String procEmetteur;
    private UUID nonConformeId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateTraitement;
    private  String observationRejet;
    private  String actionCorrective;
    @ManyToOne
    private Fichier docRejet;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateRejet;
}
