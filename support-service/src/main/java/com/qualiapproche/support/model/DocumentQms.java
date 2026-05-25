package com.qualiapproche.support.model;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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
    private String documentType; // Matches QmsDocumentType code, e.g. "PRO", "INS", etc.

    @Column(nullable = false)
    private String serviceId; // Structure/Service ID (UUID representation as string)

    private String serviceLibelle; // Structure/Service display label

    private String serviceSigle; // Structure/Service abbreviation/sigle

    @Column(nullable = false)
    private String redacteur;

    @Column(nullable = false)
    private String status; // brouillon, en_approbation, valide, archive, obsolete, en_retard_revision

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

    private String alfrescoNodeId; // Reference to node in Alfresco
    private String ncReference; // Associated NC Reference

    private String lastModifiedBy;
    private String lastModifiedReason;
    private String alerteEnvoyee; // e.g. J-60, J-30, etc.
    private boolean archived;
}
