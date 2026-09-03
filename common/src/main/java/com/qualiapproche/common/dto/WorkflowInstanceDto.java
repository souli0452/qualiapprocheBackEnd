package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Vue minimale d'une instance de validation de workflow renvoyée par workflow-service
 * (initiation, dernière instance connue pour une ressource).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Le circuit ouvert sur un dossier, réduit à ce qu'un module métier doit en "
        + "retenir après une ouverture ou une reprise. Ne dit rien de ce qui est décidable : c'est "
        + "l'état de circuit qui porte les actions offertes, lesquelles dépendent de l'appelant.")
public class WorkflowInstanceDto {
    @Schema(description = "Instance de circuit ouverte sur le dossier. Un dossier repris après "
            + "clôture en ouvre une nouvelle.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private UUID instanceId;

    @Schema(description = "Circuit que le dossier parcourt, à conserver pour reprendre le même "
            + "plutôt que d'en rouvrir un : c'est faute de cette information que la reprise "
            + "renvoyait les documents au brouillon. Nul sur les instances dont le circuit ne se "
            + "laisse plus résoudre.",
            example = "9b1f0c22-4b3e-4c2a-9a77-2f4d1b8e6c10")
    private UUID workflowId;

    @Schema(description = "Le circuit court-il encore, ou a-t-il rendu son verdict. Il ne dit pas "
            + "lequel : c'est l'étape terminale qui distingue une approbation d'un rejet.",
            example = "EN_COURS",
            allowableValues = {"EN_COURS", "TERMINE"})
    private String status;

    @Schema(description = "Identifiant technique de l'étape courante, propre à l'installation : à "
            + "transmettre et à comparer, jamais à reconnaître. Le circuit achevé, il prend la "
            + "forme « TERMINATED_APPROUVE », « TERMINATED_REJETE » ou « TERMINATED_CLOTURE ».",
            example = "42")
    private String currentStateCode;

    @Schema(description = "Étape courante telle qu'elle se présente à l'utilisateur. À défaut "
            + "d'étape résolue, le code lui sert de libellé plutôt que de laisser la place vide.",
            example = "Vérification")
    private String currentStateName;
}
