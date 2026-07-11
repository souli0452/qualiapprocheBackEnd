package com.qualiapproche.support.model;

import com.qualiapproche.common.base.AuditEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qms_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DocumentQms extends AuditEntity {

    @Column(nullable = false, unique = true)
    private String documentNumber;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String documentType; // code du type ex: "PRO", "INS", etc.

    private String reference;    // Référence officielle interne

    private String description;

    @Column(nullable = false)
    private String serviceId;

    private String serviceLibelle;
    private String serviceSigle;

    @Column(nullable = false)
    private String redacteur;

    private boolean esTraiter;
    private boolean enRetardRevision;
    private boolean obsolete;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_step_id")
    private WorkflowStep currentStep;

    @Builder.Default
    private int versionMajeure = 1;

    @Builder.Default
    private int versionMineure = 0;

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

    private String lastModifiedBy;
    private String lastModifiedReason;
    private String alerteEnvoyee;
    private boolean archived;

    // Workflow
    private String workflowStatus; // EN_COURS, TERMINE, null

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<QmsDocumentVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentUserAccess> userAccessList = new ArrayList<>();

    @JsonProperty("currentObjectName")
    public String getCurrentObjectName() {
        if (versions != null && !versions.isEmpty()) {
            return versions.get(versions.size() - 1).getObjectName();
        }
        return null;
    }

    @JsonProperty("currentFileHash")
    public String getCurrentFileHash() {
        if (versions != null && !versions.isEmpty()) {
            return versions.get(versions.size() - 1).getFileHash();
        }
        return null;
    }
}

