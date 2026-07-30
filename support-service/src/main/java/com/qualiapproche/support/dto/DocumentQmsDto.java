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
    private WorkflowStateDto workflowState;
}
