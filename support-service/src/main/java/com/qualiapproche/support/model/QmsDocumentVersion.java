package com.qualiapproche.support.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String versionLabel; // rang du document à ce dépôt : "v0", "v1"…

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private String createdBy;

    private String comment;

    @Column(nullable = false)
    private String objectName; // Nom de l'objet dans Minio (UUID + extension)

    private String originalFilename;

    private Long fileSize;

    private String fileHash;
}

