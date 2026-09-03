package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.common.utils.StatutEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Action engagée pour traiter une non-conformité. Ce que le plan doit porter "
        + "dépend du circuit retenu : en correction, la cause n'a pas à être recherchée ; en "
        + "action corrective, le plan est complet.")
public class PlanActionDto extends AuditEntityDto {

    @Schema(description = "Numéro d'ordre de l'action au sein du dossier. Attribué par le serveur "
            + "s'il n'est pas fourni.", example = "1")
    private String numeroOdre;

    @Schema(description = "Cause racine du dysfonctionnement, telle que l'analyse l'a établie. "
            + "Exigée en action corrective, facultative en correction.",
            example = "Absence de contrôle documenté lors du changement d'équipe.")
    private String causeIdentifiees;

    @Schema(description = "Solution retenue pour supprimer la cause, et non seulement ses effets.",
            example = "Checklist de transmission obligatoire entre chaque vacation.")
    private String solutionRetenues;

    @Schema(description = "Identifiant de l'agent qui répond de l'action.")
    private String responsableId;

    @Schema(description = "Nom du responsable, conservé tel qu'il était à la désignation.",
            example = "Idrissa Ouédraogo")
    private String responsableNomComplet;

    @Schema(description = "Avancement de l'action dans son circuit.", example = "NON_TRAITER")
    private StatutEnum status;

    @Schema(description = "Adresse du responsable. C'est elle qui reçoit les relances d'échéance.",
            example = "i.ouedraogo@exemple.bf")
    private String responsableEmail;

    @Schema(description = "Téléphone du responsable.", example = "70 00 00 00")
    private String numeroTelephone;

    @Schema(description = "Date limite de réalisation. Elle commande les relances et le signalement "
            + "des retards.", example = "15-05-2026")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateEcheance;

    @Schema(description = "Référence lisible de la non-conformité dont l'action relève, reprise "
            + "pour que l'action se lise sans rouvrir le dossier.", example = "NC-2026-014")
    private String numeroNc;

    @Schema(description = "Date à laquelle l'action a effectivement été achevée.")
    private LocalDate dateTraitement;

    @Schema(description = "Processus à l'origine du dossier, repris ici pour la même raison que le "
            + "numéro.", example = "Achats")
    private String procEmetteur;

    @Schema(description = "Le dossier dont l'action relève, sans ses propres actions ni ses "
            + "fichiers : l'action porte son dossier, qui porterait sinon ses actions à son tour, "
            + "et la réponse ne finirait jamais de se replier sur elle-même.")
    private NonConformiteDto nonConformite;

    @Schema(description = "Pièces justificatives déposées sur l'action.")
    private List<PieceJointeDTO> fichiers;

    @Schema(description = "Observations libres sur la conduite de l'action.")
    private String observation;

    @Schema(description = "Identifiant de la non-conformité dont l'action relève.")
    private UUID nonConformeId;

    @Schema(description = "Motif du rejet de l'action, saisi à l'étape qui l'a refusée.")
    private String observationRejet;

    @Schema(description = "Ce qui sera concrètement fait, en actes.",
            example = "Rédiger la fiche de poste et former les trois chefs d'équipe.")
    private String actionCorrective;

    @Schema(description = "Pièce déposée à l'appui du rejet.")
    private PieceJointeDTO docRejet;

    @Schema(description = "Date du rejet, le cas échéant.")
    private LocalDate dateRejet;

    @Schema(description = "Critère objectif qui permettra de dire, après coup, si l'action a "
            + "produit son effet. Fixé à la définition, non à la clôture.",
            example = "Aucune récidive constatée sur les trois audits suivants.")
    private String critereEfficacite;

    /**
     * Ce que le responsable qualité a observé, confronté au critère d'efficacité fixé à la
     * définition de l'action. Recueilli par l'étape « Efficacité à mesurer » du circuit.
     */
    @Schema(description = "Ce qui a été constaté, confronté au critère ci-dessus. Recueilli par "
            + "l'étape « Efficacité à mesurer ».",
            example = "Contrôle du 15/10 : aucun écart sur 45 transmissions.")
    private String constatEfficacite;

    @Schema(description = "Identifiant de l'instance de circuit qui suit l'action.")
    private UUID workflowId;

    @Schema(description = "Nom de l'étape courante du circuit.", example = "Traitement")
    private String workflowStatus;

    /** État du circuit de validation du plan d'action (cf. {@link NonConformiteDto#getWorkflowState()}). */
    @Schema(description = "État du circuit : étape courante, décisions ouvertes à l'appelant et "
            + "champs qu'elles réclament. Sans lui, l'écran annoncerait une action à traiter sans "
            + "pouvoir offrir aucun geste.")
    private WorkflowStateDto workflowState;
}
