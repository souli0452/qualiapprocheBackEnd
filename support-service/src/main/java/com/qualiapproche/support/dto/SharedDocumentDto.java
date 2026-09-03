package com.qualiapproche.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Document auquel une personne accède par un partage, et non par sa "
        + "structure. La ligne joint au document les conditions de cet accès : à qui il est "
        + "consenti, avec quel rôle, et par quelle voie.")
public class SharedDocumentDto {

    /** ID du document */
    @Schema(description = "Identifiant du document partagé, à citer pour le consulter ou le "
            + "télécharger.")
    private UUID documentId;

    /** Numéro du document (ex: PRO-DSI-2024-001) */
    @Schema(description = "Numéro attribué par le système, seul repère unique de l'enregistrement.",
            example = "PRO-DSI-2024-001")
    private String documentNumber;

    /** Titre du document */
    @Schema(description = "Intitulé du document.",
            example = "Procédure de maîtrise des enregistrements")
    private String titre;

    /** Code du type (PRO, INS, FOR, ...) */
    @Schema(description = "Code du type au référentiel documentaire.", example = "PRO")
    private String documentType;

    /** Statut courant (brouillon, valide, ...) */
    @Schema(description = "Statut affiché, en minuscules, déduit des drapeaux du document. Un "
            + "document encore en circuit est rendu sous le code de son étape : l'ensemble des "
            + "valeurs n'est donc pas fixe.",
            example = "valide")
    private String status;

    /** Service propriétaire */
    @Schema(description = "Structure qui a émis le document — celle qui a consenti le partage, et "
            + "non celle du bénéficiaire.",
            example = "Direction des systèmes d'information")
    private String serviceLibelle;

    @Schema(description = "Sigle de cette structure émettrice.", example = "DSI")
    private String serviceSigle;

    /** Rédacteur principal */
    @Schema(description = "Rédacteur déclaré au dépôt. Renseignement d'affichage, sans effet sur "
            + "les droits.")
    private String redacteur;

    /** Domaine fonctionnel */
    @Schema(description = "Libellé du domaine d'application.", example = "Qualité")
    private String domaine;

    /** Version actuelle (ex: "2.0") */
    @Schema(description = "Rang de révision en cours, formé du rang précédé d'un « v ». Un "
            + "document déposé et validé reste en v0 : seule une demande de modification aboutie "
            + "le fait monter.",
            example = "v2")
    private String versionLabel;

    /** Date de mise en vigueur */
    @Schema(description = "Date d'entrée en application. Nulle tant que le circuit n'a pas abouti.")
    private LocalDateTime dateVigueur;

    /** Date de prochaine révision */
    @Schema(description = "Échéance de la revue périodique. Nulle lorsque le document n'est soumis "
            + "à aucune périodicité.")
    private LocalDateTime dateProchRevision;

    /** Indique si le document est confidentiel */
    @Schema(description = "Le document porte un niveau de confidentialité. Un partage nominatif "
            + "passe outre ce classement ; un partage à la structure y reste soumis.",
            example = "false")
    private boolean confidentiel;

    // ---- Informations d'accès ----

    /** ID Keycloak de l'utilisateur ayant l'accès */
    @Schema(description = "Bénéficiaire de l'accès, par son identifiant Keycloak. C'est lui, et "
            + "non l'appelant, que la ligne décrit.")
    private String userId;

    /** Nom complet de l'utilisateur */
    @Schema(description = "Nom du bénéficiaire, relevé au moment du partage.")
    private String userFullName;

    /** Email de l'utilisateur */
    @Schema(description = "Adresse du bénéficiaire, relevée au moment du partage : elle sert à "
            + "l'affichage et non à l'envoi.")
    private String userEmail;

    /** Rôle sur ce document : READ_ONLY ou WRITE */
    @Schema(description = "Étendue du droit consenti. Il ne vaut que sur ce document et ne "
            + "s'étend à aucun autre de la même structure.",
            example = "READ_ONLY",
            allowableValues = {"READ_ONLY", "WRITE"})
    private String accessRole;

    /**
     * Vrai lorsque l'accès vient d'un partage consenti à la structure entière, et non d'une
     * désignation nominative. L'écran s'en sert pour dire d'où vient le droit, et pour ne pas
     * proposer un suivi interne que le serveur refuserait.
     */
    @Schema(description = "L'accès vient d'un partage consenti à la structure entière, et non "
            + "d'une désignation nominative. La distinction compte : le partage nominatif lève le "
            + "classement du document, celui à la structure non.",
            example = "true")
    private boolean partageStructure;

    /** Qui a partagé, lorsque le partage vise la structure. */
    @Schema(description = "Auteur du partage, renseigné pour les seuls partages de structure. Vide "
            + "sur un partage nominatif.")
    private String partagePar;
}
