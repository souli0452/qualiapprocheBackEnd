package com.qualiapproche.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import com.qualiapproche.common.dto.WorkflowStateDto;

/** Demande de modification ou de suppression, telle que la présentent les écrans. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Demande de modification ou de suppression portant sur un document déjà "
        + "déposé. Elle suit son propre circuit de validation, distinct de celui du document, et "
        + "survit à son aboutissement : c'est la seule trace de ce qui a été décidé et pourquoi.")
public class DemandeDocumentDto {

    @Schema(description = "Identifiant de la demande, à ne pas confondre avec celui du document "
            + "qu'elle vise.")
    private UUID id;

    @Schema(description = "Document visé. La demande ne le référence que par cet identifiant : le "
            + "document peut être retiré, la demande lui survit.")
    private UUID documentId;

    @Schema(description = "Numéro du document au moment du dépôt, recopié ici pour que la trace "
            + "reste lisible même après le retrait du document.",
            example = "PRO-DSI-2024-001")
    private String documentNumber;

    @Schema(description = "Titre du document au moment du dépôt, recopié pour la même raison que "
            + "le numéro.")
    private String documentTitre;

    @Schema(description = "Nature de la demande, qui commande son aboutissement : une modification "
            + "acceptée ouvre le dépôt d'un fichier remplaçant, une suppression acceptée retire le "
            + "document.",
            example = "MODIFICATION",
            allowableValues = {"MODIFICATION", "SUPPRESSION"})
    private String type;

    @Schema(description = "Avancement de la demande du point de vue documentaire, distinct de "
            + "l'étape du circuit. Une demande acceptée reste à exécuter tant que le geste qu'elle "
            + "ouvre n'a pas été fait.",
            example = "ACCEPTEE",
            allowableValues = {"EN_COURS", "ACCEPTEE", "REFUSEE", "EXECUTEE"})
    private String etat;

    @Schema(description = "Ce que le demandeur cherche à obtenir, en une phrase. C'est le texte que "
            + "lisent les décideurs du circuit.",
            example = "Mettre à jour la procédure après la réorganisation du service")
    private String objectif;

    @Schema(description = "Exposé détaillé à l'appui de l'objectif, facultatif.")
    private String description;

    @Schema(description = "Structure du demandeur, relevée à la soumission et jamais saisie. Elle "
            + "borne qui recevra les courriels d'étape et qui pourra décider.")
    private String structureId;

    @Schema(description = "Libellé de cette structure, pour l'affichage seul.",
            example = "Direction des systèmes d'information")
    private String structureLibelle;

    @Schema(description = "Identifiant Keycloak de l'auteur de la demande, relevé de la session.")
    private String demandeurId;

    @Schema(description = "Nom de l'auteur tel qu'il se présentait au dépôt, conservé pour que la "
            + "trace reste lisible si le compte change ou disparaît.")
    private String demandeurNom;

    @Schema(description = "Nom d'origine du fichier déposé à l'appui de la demande, ou vide s'il "
            + "n'y en a pas eu. Le contenu ne circule pas ici.",
            example = "Constat_terrain.pdf")
    private String pieceJointeNom;

    @Schema(description = "Circuit de validation suivi par la demande, de la famille "
            + "DEMANDE_DOCUMENT, et non celui du document visé.")
    private UUID workflowId;

    @Schema(description = "Étape où le circuit est arrêté, telle que la nomme workflow-service. "
            + "Vide tant qu'aucun circuit n'a été ouvert.",
            example = "VALIDATION_PILOTE")
    private String currentEtape;

    @Schema(description = "Date du dépôt de la demande.")
    private LocalDateTime createdAt;

    @Schema(description = "Date à laquelle le circuit a rendu sa décision finale. Nulle tant que "
            + "la demande est en cours.")
    private LocalDateTime dateDecision;

    @Schema(description = "Motif de la décision finale, repris du commentaire porté par la "
            + "dernière transition du circuit — il n'est pas saisi séparément.")
    private String motifDecision;

    @Schema(description = "Date à laquelle l'aboutissement a été constaté : dépôt du fichier "
            + "remplaçant, ou retrait effectif du document. Elle suit la décision, parfois de "
            + "plusieurs jours.")
    private LocalDateTime dateExecution;

    /**
     * Vrai lorsque la demande est acceptée et attend son exécution : dépôt du fichier remplaçant
     * pour une modification, confirmation du retrait pour une suppression.
     */
    @Schema(description = "La demande est acceptée mais son effet reste à produire : dépôt du "
            + "fichier remplaçant pour une modification, confirmation du retrait pour une "
            + "suppression. C'est ce drapeau, et non l'étape du circuit, qui commande le bouton "
            + "d'exécution.",
            example = "true")
    private boolean enAttenteExecution;

    /**
     * État du circuit : étape courante, décisions ouvertes à l'appelant, champs à renseigner.
     *
     * <p>Renseigné par les listes qui proposent d'agir — la vue d'ensemble — et nul ailleurs. Une
     * liste qui annonce des décisions à prendre sans porter les actions du moteur oblige à ouvrir
     * chaque fiche pour découvrir ce qu'on peut y faire ; et les demander une par une multipliait
     * les allers-retours autant que de lignes.</p>
     */
    @Schema(description = "État du circuit : étape courante, décisions ouvertes à l'appelant, "
            + "champs à renseigner. Renseigné par les listes qui proposent d'agir, et nul "
            + "ailleurs — ne pas s'y fier pour savoir si la demande a un circuit.")
    private WorkflowStateDto workflowState;
}
