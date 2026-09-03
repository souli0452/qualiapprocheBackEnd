package com.qualiapproche.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Circuit de validation paramétrable : la suite d'étapes que parcourt un "
        + "dossier, avec les décisions offertes à chacune. Il est enregistré d'un seul tenant, "
        + "étapes et transitions comprises — ce que l'envoi omet est retiré du circuit.")
public class WorkflowDto {

    @Schema(description = "Identifiant du circuit. Absent à la création, il est alors attribué par "
            + "le serveur.")
    private UUID id;

    @Schema(description = "Nom du circuit, tel qu'il se présente dans l'éditeur et dans les "
            + "messages d'erreur.",
            example = "Validation documentaire")
    private String nom;

    @Schema(description = "À quoi sert ce circuit et dans quel cas le choisir, à l'usage de "
            + "l'administrateur qui configure.")
    private String description;

    @Schema(description = "Famille de ressources à laquelle le circuit s'applique. Ce n'est pas le "
            + "type précis du dossier : un circuit réservé à un type de document reste de famille "
            + "DOCUMENT et se désigne depuis ce type. La casse et les espaces sont tolérés, mais "
            + "toute autre valeur est refusée dès la configuration.",
            example = "DOCUMENT",
            allowableValues = {"DOCUMENT", "NON_CONFORMITE", "PLAN_ACTION", "DEMANDE_DOCUMENT"})
    private String resourceType;

    @Schema(description = "Le circuit peut être ouvert sur de nouveaux dossiers. Le désactiver "
            + "n'arrête pas ceux qui le parcourent déjà.",
            example = "true")
    @Builder.Default
    private boolean actif = true;

    /**
     * Entité à laquelle le circuit est réservé au sein de sa famille — l'identifiant d'un type de
     * document, par exemple — ou vide s'il est le circuit par défaut de la famille.
     *
     * <p>Vide et {@code null} valent la même chose : un champ effacé dans l'éditeur arrive en chaîne
     * vide, et l'écran ne doit pas avoir à connaître cette nuance pour rendre un circuit à la
     * famille entière.</p>
     */
    @Schema(description = "Entité à laquelle le circuit est réservé au sein de sa famille — un "
            + "type de document, par exemple. Vide, il vaut pour la famille entière ; la chaîne "
            + "vide et l'absence de valeur ont ici le même sens.")
    private String cibleId;

    @Schema(description = "Étapes du circuit, dans l'ordre que fixe leur rang. Le circuit se "
            + "sauvegarde avec elles : une étape absente de l'envoi est supprimée, avec ses "
            + "transitions et ses champs. Seule exception, une étape où des dossiers se trouvent, "
            + "dont le retrait est refusé.")
    @Builder.Default
    private List<WorkflowStepDto> steps = new ArrayList<>();
}
