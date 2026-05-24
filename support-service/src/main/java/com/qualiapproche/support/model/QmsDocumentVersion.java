package com.qualiapproche.support.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "qms_document_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QmsDocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private String versionLabel; // e.g. "1.0", "1.1"

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private String createdBy;

    private String comment;

    @Column(nullable = false)
    private String alfrescoNodeId; // Alfresco node ID of the archived file version
}
