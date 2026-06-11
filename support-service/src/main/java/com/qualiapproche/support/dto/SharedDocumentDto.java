package com.qualiapproche.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vue d'un document partagé avec un utilisateur, incluant son rôle d'accès.
 * Retourné par GET /documents/shared/{userId}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedDocumentDto {

    /** ID du document */
    private UUID documentId;

    /** Numéro du document (ex: PRO-DSI-2024-001) */
    private String documentNumber;

    /** Titre du document */
    private String titre;

    /** Code du type (PRO, INS, FOR, ...) */
    private String documentType;

    /** Statut courant (brouillon, valide, ...) */
    private String status;

    /** Service propriétaire */
    private String serviceLibelle;
    private String serviceSigle;

    /** Rédacteur principal */
    private String redacteur;

    /** Domaine fonctionnel */
    private String domaine;

    /** Version actuelle (ex: "2.0") */
    private String versionLabel;

    /** Date de mise en vigueur */
    private LocalDateTime dateVigueur;

    /** Date de prochaine révision */
    private LocalDateTime dateProchRevision;

    /** Indique si le document est confidentiel */
    private boolean confidentiel;

    // ---- Informations d'accès ----

    /** ID Keycloak de l'utilisateur ayant l'accès */
    private String userId;

    /** Nom complet de l'utilisateur */
    private String userFullName;

    /** Email de l'utilisateur */
    private String userEmail;

    /** Rôle sur ce document : READ_ONLY ou WRITE */
    private String accessRole;
}
