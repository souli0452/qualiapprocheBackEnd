package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ce qui accompagne une décision prise sur un dossier. L'action elle-même "
        + "n'est pas ici : elle est nommée par le point d'entrée appelé ou par le code de "
        + "transition dans l'adresse.")
public class WorkflowValidationRequestDto {
    @Schema(description = "Ce que le décideur a à dire sur sa décision. Conservé dans l'historique "
            + "du dossier, où il reste lisible après coup ; sert notamment de motif à un renvoi.",
            example = "Le paragraphe 3 renvoie à une procédure abrogée.")
    private String comments;

    @Schema(description = "Valeurs saisies, indexées par l'identifiant du champ tel que l'étape "
            + "courante le publie — et non par son nom technique, qui n'est pas la clé attendue. Un "
            + "identifiant inconnu fait échouer la demande. Seuls sont exigés les champs que "
            + "l'action prise réclame ; les autres peuvent être omis.",
            example = "{\"58\": \"Conforme après reprise\"}")
    private Map<Long, String> fields;

    /**
     * Étape sur laquelle l'appelant croit agir ({@code currentStateCode} de l'état qu'il a affiché).
     *
     * <p>Si elle est fournie et ne correspond plus à l'étape réelle, la demande est rejetée en 409 :
     * c'est ce qui empêche un double envoi de franchir deux transitions d'affilée, ou un utilisateur
     * de décider à partir d'un écran périmé. Facultative pour rester compatible avec les appelants
     * qui ne la transmettent pas encore.</p>
     */
    @Schema(description = "Étape sur laquelle l'appelant croit agir, reprise de l'état qu'il a "
            + "affiché. Si elle ne correspond plus à l'étape réelle, la demande est refusée en 409 : "
            + "c'est ce qui arrête le second envoi d'un double clic et la décision prise depuis un "
            + "écran périmé. Facultative, et le contrôle ne s'applique qu'à qui la transmet.",
            example = "42")
    private String expectedStateCode;
}
