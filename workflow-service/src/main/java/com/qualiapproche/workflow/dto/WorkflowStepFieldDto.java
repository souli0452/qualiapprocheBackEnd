package com.qualiapproche.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Un champ que l'étape demande au moment de décider. Ce qui y est saisi reste "
        + "attaché au dossier et le suit jusqu'au bout de son circuit ; aucun module métier n'a de "
        + "colonne à prévoir pour l'accueillir.")
public class WorkflowStepFieldDto {

    @Schema(description = "Identifiant du champ. C'est par lui que la requête de validation indexe "
            + "les valeurs envoyées, et non par le nom du champ.",
            example = "42")
    private Long id;

    @Schema(description = "Nom technique du champ, sous lequel la valeur sera relue. Un module "
            + "métier qui attend une donnée précise la cherche par ce nom.",
            example = "userImputId")
    private String fieldName;

    @Schema(description = "Intitulé présenté à qui saisit. Conservé avec chaque valeur, si bien "
            + "qu'un champ renommé n'obscurcit pas les saisies passées.",
            example = "Agent chargé du traitement")
    private String fieldLabel;

    @Schema(description = "Nature de la saisie, sous le nom de la constante. La lecture tolère la "
            + "casse, mais toute autre valeur est refusée.",
            example = "SELECT",
            allowableValues = {"TEXT", "NUMERIC", "SELECT", "FILE", "DATE"})
    private String type; // string, numeric, select, file, date

    @Schema(description = "La décision est refusée si le champ reste vide. Contrôlé au serveur, et "
            + "non seulement à l'écran.",
            example = "true")
    private boolean required;

    @Schema(description = "Choix d'un champ SELECT : soit la liste littérale des libellés, soit "
            + "une source à interroger, reconnaissable à son arobase — @STRUCTURES, @UTILISATEURS, "
            + "@UTILISATEURS_MA_STRUCTURE, @CIRCUITS_TRAITEMENT. Une source retient l'identifiant "
            + "comme valeur là où une liste littérale retient le libellé, qu'une correction "
            + "d'orthographe suffirait à invalider.",
            example = "@UTILISATEURS_MA_STRUCTURE")
    private String options;

    /**
     * Décision à laquelle le champ se rapporte ({@code APPROUVE}, {@code REJETE}), ou {@code null}
     * s'il vaut quelle que soit la décision. L'écran n'a ainsi à présenter que ce que la décision
     * choisie réclame.
     */
    @Schema(description = "Nature de décision à laquelle le champ se rapporte, ou rien s'il vaut "
            + "pour toutes. Un justificatif de rejet demandé sans cette portée serait exigé de qui "
            + "approuve.",
            example = "REJETE",
            allowableValues = {"APPROUVE", "REJETE", "CLOTURE"})
    private String decision;

    /**
     * Code de l'action qui, seule, réclame ce champ — ou {@code null} s'il vaut pour toutes celles
     * que sa décision laisse passer.
     *
     * <p>Nécessaire dès qu'une étape offre plusieurs actions de même nature : sans lui, le motif
     * demandé par « Demander un complément » se présenterait aussi à qui valide simplement.</p>
     */
    @Schema(description = "Action précise qui, seule, réclame ce champ. Les deux portées se "
            + "cumulent et la plus étroite l'emporte : un champ qui nomme une action n'est demandé "
            + "que par elle, quelle que soit la décision indiquée au-dessus.",
            example = "DEMANDER_COMPLEMENT")
    private String actionCode;
}
