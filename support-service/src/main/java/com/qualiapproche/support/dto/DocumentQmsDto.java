package com.qualiapproche.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import com.qualiapproche.common.dto.WorkflowStateDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQmsDto {
    private UUID id;
    private String documentNumber;
    private String titre;
    private String documentType;
    private String reference;
    private String description;
    private String serviceId;
    private String serviceLibelle;
    private String serviceSigle;
    private String redacteur;
    private boolean esTraiter;
    private boolean enRetardRevision;
    private boolean obsolete;
    private int versionMajeure;
    private int versionMineure;
    private LocalDateTime dateVigueur;
    private LocalDateTime dateProchRevision;
    private Integer periodiciteMois;
    private boolean confidentiel;
    private boolean documentExterne;
    private String processusDestId;
    private String processusDestLibelle;
    private String referenceOfficielle;
    private LocalDateTime datePublication;
    private String domaine;
    private String statutLegal;
    private String ncReference;
    private String currentEtape;
    private UUID workflowId;
    private String currentObjectName;
    private String currentFileHash;
    private LocalDateTime createdAt;
    private String createdById;
    private String currentUserfullName;
    // `reference` figure déjà plus haut : c'est le code saisi par l'auteur, selon la convention de
    // numérotation de l'organisation. Il était porté par le DTO sans qu'aucun écran ne l'offre.

    private String prioriteId;
    private String prioriteLibelle;
    private String niveauConfidentialiteId;
    private String niveauConfidentialiteLibelle;
    private String domaineId;

    private WorkflowStateDto workflowState;

    /**
     * Vrai si l'appelant relève de la structure du document (ou l'accompagne au titre de la
     * qualité), faux s'il n'y accède que par un partage.
     *
     * <p>Décidé par le serveur, qui seul connaît la structure de chacun, et transmis pour que
     * l'écran ne propose pas un historique, une piste d'audit ou une traçabilité que le serveur
     * refuserait — un bouton qui mène à un refus est pire que pas de bouton.</p>
     */
    private boolean suiviInterneAutorise;
}
