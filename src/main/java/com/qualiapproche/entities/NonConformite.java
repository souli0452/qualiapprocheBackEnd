
package com.qualiapproche.entities;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.enumeration.Etat;
import com.qualiapproche.enumeration.Status;
import com.qualiapproche.enumeration.TypeDemande;
import jakarta.persistence.*;
import lombok.*;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private String justification;
    private UUID efficaciteId;
    private  String userImputeEmail;
    private UUID niveauNonConformiteId;
    private UUID actionId;
    private String structureSoumissionId;
    private String structureSoumissionLibelle;
    private String actionLibelle;
    private  String typeNonConformiteLibelle;
    private  String niveauNonConformiteLibelle;
    private  String efficaciteLibelle;
    private  String typeProcessusLibelle;
    private UUID typeNonConformiteId;
    private UUID typeProcessusId;
    private String delaisMiseOeuvre;
    @Enumerated(EnumType.STRING)
    private Etat etatTraitement;
    @Enumerated(EnumType.STRING)
    private TypeDemande typeDemande ;
    private String observationsRq;
    private String dateObservationsRq;
    private String observationsCloture;
    private String dateVerification;
    private String structureResponsableId;
    private String structureResponsableSigle;
    private String structureResponsableLibelle;
    private String dispositionPreventives;
    private String dateClotureRq;
    @Enumerated(EnumType.STRING)
    private Status status;
    private  String pertinanceRs;
    private  String justificationRs;
    private  String pertinancePilote;
    private  String justificationPilote;
    private  String userImputId;
    private  String userImputFullName;
    private  String originNonConformiteId;
    private  String originNonConformiteLibelle;
    private String numeroFdac;
    private String pertinanceRsSuivi;
    private  String actionDsc;
    private  String observationRejet;
    @ManyToOne
    private Fichier docRejet;
    @OneToMany
    private List<Fichier> fichiers;
    //@OneToMany(mappedBy = "nonConformite", cascade = CascadeType.ALL, orphanRemoval = true)
    //private List<PlanAction> planActions;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanAction> planActions = new ArrayList<>();
    @Embedded
    private Participants participants = new Participants();
}
