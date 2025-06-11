package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.entities.*;
import com.qualiapproche.enumeration.Etat;
import com.qualiapproche.enumeration.Status;
import com.qualiapproche.enumeration.TypeDemande;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.experimental.SuperBuilder;
import org.springframework.transaction.annotation.Transactional;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
@Transactional
public class NonConformiteDto extends AuditEntityDto {

    private String numeroReference;
    private String nomProcessus;
    private String numeroFdac;
    private String pertinanceRsSuivi;
    private String origineService;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime dateSuivi;
    private String origineId;
    private String fonctionEmetteur;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime dateVisaEmetteur;
    private String justification;
    private String delaisMiseOeuvre;
    @Enumerated(EnumType.STRING)
    private Etat etatTraitement;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String observationsRq;
    private String structureSoumissionId;
    private String structureSoumissionLibelle;
    private String dateObservationsRq;
    private String observationsCloture;
    private String dateVerification;
    private String dispositionPreventives;
    private String dateClotureRq;
    // On stocke uniquement les ID des objets associés
    private UUID efficaciteId;
    private UUID niveauNonConformiteId;
    private UUID actionId;
    private UUID typeNonConformiteId;
    private UUID typeProcessusId;
    @Enumerated(EnumType.STRING)
    private TypeDemande typeDemande ;
    private String actionLibelle;
    private  String typeNonConformiteLibelle;
    private  String niveauNonConformiteLibelle;
    private  String efficaciteLibelle;
    private  String typeProcessusLibelle;
    private String version;
    private List<Fichier> fichiers;
    private List<PlanActionDto> planActions;
    private String structureResponsableId;
    private String structureResponsableSigle;
    private String structureResponsableLibelle;
    private  String pertinanceRs;
    private  String justificationRs;
    private  String pertinancePilote;
    private  String justificationPilote;
    private  String userImputId;
    private  String userImputFullName;
    private  String userImputeEmail;
    private Set<String> participants=new HashSet<>();
}
