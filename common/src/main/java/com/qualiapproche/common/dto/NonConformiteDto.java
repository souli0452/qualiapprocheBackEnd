package com.qualiapproche.common.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.common.enumeration.Circuit;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private String nomProcessus;
    private String numeroFdac;
    private String pertinanceRsSuivi;
    private String origineService;
    private String origineServiceLibelleCourt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime publicationDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime archivageDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime dateSuivi;
    private String origineId;
    private String fonctionEmetteur;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime dateVisaEmetteur;
    private String delaisMiseOeuvre;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String observationRq;
    private String dateObservationsRq;
    private String observationsCloture;
    private String dateVerification;
    private String dispositionPreventives;
    private String dateClotureRq;
    private UUID efficaciteId;
    private UUID niveauNonConformiteId;
    private UUID actionId;
    @Enumerated(EnumType.STRING)
    private TypeDemande typeDemande;
    private String actionLibelle;
    private String niveauNonConformiteLibelle;
    private String efficaciteLibelle;
    private String version;
    private List<PieceJointeDTO> fichiers;
    private List<PlanActionDto> planActions;
    private String structureResponsableId;
    private String structureResponsableSigle;
    private String structureResponsableLibelle;
    private Set<String> participants = new HashSet<>();
    private String originNonConformiteId;
    private String originNonConformiteLibelle;
    private String observationRejet;
    private PieceJointeDTO docRejet;
    @Enumerated(EnumType.STRING)
    private Circuit circuit;
    private String actionPreventive;
    private String workflowStatus;
    private UUID workflowId;

    /**
     * État du circuit de validation : étape courante, actions autorisées pour l'utilisateur
     * appelant et champs à saisir. Alimenté à la consultation d'une non-conformité, à l'image
     * de ce que fait déjà le service documentaire — sans quoi les écrans ne pouvaient afficher
     * aucune action de workflow.
     */
    private WorkflowStateDto workflowState;

    // --- NOUVELLES APPELLATIONS CLAIRES (AI-READY) ---
    private String numeroDeReference;
    private Etat etatDeTraitement;
    private String structureDeSoumissionId;
    private String structureDeSoumissionLibelle;
    private String description;
    private UUID sourceDeNonConformiteId;
    private String sourceDeNonConformiteLibelle;
    private UUID categorieProcessusId;
    private String categorieProcessusLibelle;
    private String agentImputeId;
    private String agentImputeNomComplet;
    private String agentImputeEmail;
    private String actionImmediate;
    private String pertinencePilote;
    private String pertinenceRs;

        private String observationPilote;
    private String observationRs;
}

