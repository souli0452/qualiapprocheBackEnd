package com.qualiapproche.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

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
    private String status;
    private boolean esTraiter;
    private boolean enRetardRevision;
    private boolean obsolete;
    private WorkflowStepDto currentStep;
    private int versionMajeure;
    private int versionMineure;
    private LocalDateTime dateVigueur;
    private LocalDateTime dateProchRevision;
    private Integer periodiciteMois;
    private boolean confidentiel;
    private boolean documentExterne;
    private String organismeEmetteur;
    private String referenceOfficielle;
    private LocalDateTime datePublication;
    private String domaine;
    private String statutLegal;
    private String ncReference;
    private String workflowStatus;
    private String currentObjectName;
    private String currentFileHash;
    private LocalDateTime createdAt;
    private String createdById;
    private String currentUserfullName;
}
