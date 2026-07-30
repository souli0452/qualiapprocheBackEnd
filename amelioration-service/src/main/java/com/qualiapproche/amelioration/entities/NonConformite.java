package com.qualiapproche.amelioration.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.common.enumeration.Circuit;
import com.qualiapproche.common.base.Participants;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "quali_nc")
public class NonConformite extends AuditEntity {
  private String numeroReference;
  private String version;
  private String origineId;
  private String nomProcessus;
  private String origineService;
  private String origineServiceLibelleCourt;
  private String fonctionEmetteur;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
  private LocalDateTime dateVisaEmetteur;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
  private LocalDateTime dateSuivi;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
  private LocalDateTime publicationDate;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
  private LocalDateTime archivageDate;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String justification;
  private UUID efficaciteId;
  private String userImputeEmail;
  private UUID niveauNonConformiteId;
  private UUID actionId;
  private String structureSoumissionId;
  private String structureSoumissionLibelle;
  private String actionLibelle;
  private String typeNonConformiteLibelle;
  private String niveauNonConformiteLibelle;
  private String efficaciteLibelle;
  private String typeProcessusLibelle;
  private UUID typeNonConformiteId;
  private UUID typeProcessusId;
  private String delaisMiseOeuvre;
  
  @Column(name = "workflow_status")
  private String workflowStatus;
  
  @Column(name = "workflow_id")
  private UUID workflowId;

  @Enumerated(EnumType.STRING)
  private Etat etatTraitement;
  @Enumerated(EnumType.STRING)
  private TypeDemande typeDemande;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String observationsRq;
  private String dateObservationsRq;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String observationsCloture;
  private String dateVerification;
  private String structureResponsableId;
  private String structureResponsableSigle;
  private String structureResponsableLibelle;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String dispositionPreventives;
  private String dateClotureRq;
  @Enumerated(EnumType.STRING)
  private Status status;
  private String pertinanceRs;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String justificationRs;
  private String pertinancePilote;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String justificationPilote;
  private String userImputId;
  private String userImputFullName;
  private String originNonConformiteId;
  private String originNonConformiteLibelle;
  private String numeroFdac;
  private String pertinanceRsSuivi;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String actionDsc;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String observationRejet;
  @Lob
  @Column(columnDefinition = "TEXT")
  private String actionPreventive;
  @Enumerated(EnumType.STRING)
  private Circuit circuit;
  @ManyToOne
  private PieceJointe docRejet;
  @OneToMany
  private List<PieceJointe> fichiers;
  // @OneToMany(mappedBy = "nonConformite", cascade = CascadeType.ALL,
  // orphanRemoval = true)
  // private List<PlanAction> planActions;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PlanAction> planActions = new ArrayList<>();
  @Embedded
  private Participants participants = new Participants();
}