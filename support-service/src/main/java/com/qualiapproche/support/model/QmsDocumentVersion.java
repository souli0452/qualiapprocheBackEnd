package com.qualiapproche.support.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @JsonIgnore
    private DocumentQms document;

    @Column(nullable = false)
    private String versionLabel; // e.g. "1.0", "1.1"

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private String createdBy;

    private String comment;

    @Column(nullable = false)
    private String objectName; // Nom de l'objet dans Minio (UUID + extension)

    private String originalFilename;

    private Long fileSize;
}
