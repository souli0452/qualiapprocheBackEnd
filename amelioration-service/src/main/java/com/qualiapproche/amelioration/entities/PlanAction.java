/*
package com.qualiapproche.amelioration.entities;
import com.qualiapproche.amelioration.entities.Fichier;
import com.qualiapproche.common.base.AuditEntity;

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

    @lombok.Getter @lombok.Setter private String numeroOdre;
    @lombok.Getter @lombok.Setter private String causeIdentifiees;
    @lombok.Getter @lombok.Setter private String solutionRetenues;
    @lombok.Getter @lombok.Setter private String responsable;
    @lombok.Getter @lombok.Setter private String mail;
    @lombok.Getter @lombok.Setter private String numeroTelephone;
    @lombok.Getter @lombok.Setter private String dateEcheance;
    @ManyToOne
    @JoinColumn(name = "non_conformite_id", nullable = false)
    @lombok.Getter @lombok.Setter private NonConformite nonConformite;
}
*/

package com.qualiapproche.amelioration.entities;

import com.qualiapproche.common.base.AuditEntity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.qualiapproche.common.utils.StatutEnum;
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
  @Lob
  @Column(columnDefinition = "TEXT")
  private String observation;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String causeIdentifiees;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String solutionRetenues;
  private String responsableNomComplet;
  private UUID responsableId;
  private String responsableEmail;
  private String numeroTelephone;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
  private LocalDate dateEcheance;
  /*
   * @OneToMany
   * private List<Fichier> fichiers;
   */
  @Enumerated(EnumType.STRING)
  private StatutEnum status;
  private String numeroNc;
  private String procEmetteur;
  private UUID nonConformeId;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
  private LocalDate dateTraitement;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String observationRejet;
  private String actionCorrective;
  /*
   * @ManyToOne
   * private Fichier docRejet;
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
  private LocalDate dateRejet;
  @Column(columnDefinition = "TEXT")
  private String critereEfficacite;
  private UUID workflowId;
  private String workflowStatus;
}
