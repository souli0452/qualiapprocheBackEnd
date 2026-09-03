package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Champ de saisie exigé par une étape de workflow.
 *
 * <p>Exposé dans {@link WorkflowStateDto} pour que l'appelant puisse construire le formulaire
 * de décision : {@code id} est la clé attendue dans {@code WorkflowValidationRequestDto.fields}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Une donnée qu'une étape réclame au moment de décider. L'administrateur les "
        + "ajoute depuis l'éditeur de circuits sans livraison ni colonne nouvelle : l'appelant "
        + "construit son formulaire sur cette description, il ne connaît aucun de ces champs "
        + "d'avance.")
public class WorkflowStepFieldDto {
    @Schema(description = "Clé sous laquelle renvoyer la valeur saisie, dans la table « fields » de "
            + "la demande de validation. C'est cet identifiant, et non le nom du champ, que le "
            + "moteur attend.",
            example = "58")
    private Long id;

    @Schema(description = "Nom technique du champ, stable et conservé avec chaque valeur saisie. "
            + "Un module métier qui s'intéresse à une donnée précise la reconnaît par lui.",
            example = "observationsVerification")
    private String fieldName;

    @Schema(description = "Intitulé à présenter au-dessus de la saisie, dans les termes de l'auteur "
            + "du circuit.",
            example = "Observations du vérificateur")
    private String fieldLabel;

    @Schema(description = "Nature de la saisie attendue, qui commande le composant à afficher.",
            example = "SELECT",
            allowableValues = {"TEXT", "NUMERIC", "SELECT", "FILE", "DATE"})
    private String type;

    @Schema(description = "Le moteur refuse la décision si la valeur est absente ou vide. "
            + "L'exigence ne porte que sur les champs que la décision prise réclame : un champ "
            + "propre au rejet ne bloque pas une approbation.",
            example = "true")
    private boolean required;

    /**
     * Liste de choix : soit les valeurs séparées par des virgules, soit une source dont elles sont
     * issues ({@code @STRUCTURES}, {@code @UTILISATEURS}).
     */
    @Schema(description = "Ce que propose une liste de choix, sous deux formes : les valeurs "
            + "littérales séparées par des virgules, ou une source à interroger — « @STRUCTURES », "
            + "« @UTILISATEURS », « @UTILISATEURS_MA_STRUCTURE », « @CIRCUITS_TRAITEMENT ». Une "
            + "source rend des identifiants, une liste littérale rend les libellés eux-mêmes. Vide "
            + "hors des champs de type SELECT.",
            example = "@UTILISATEURS_MA_STRUCTURE")
    private String options;

    /**
     * Décision à laquelle ce champ se rapporte ({@code APPROUVE}, {@code REJETE}), ou {@code null}
     * s'il vaut quelle que soit la décision.
     *
     * <p>Sans cette information, un justificatif de rejet se présentait aussi à qui approuvait :
     * on lui demandait de motiver un refus qu'il n'était pas en train de prononcer. Le champ
     * traverse ce DTO à chaque état de circuit rendu à un module métier — l'omettre ici suffisait
     * à perdre la portée entre le moteur et l'écran.</p>
     */
    @Schema(description = "Nature de décision à laquelle ce champ se limite, ou vide s'il vaut pour "
            + "toutes. Sans ce tri, un justificatif de rejet se présente à qui approuve : on lui "
            + "demande de motiver un refus qu'il n'est pas en train de prononcer.",
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
    @Schema(description = "Action unique qui réclame ce champ, désignée par son code, ou vide s'il "
            + "vaut pour toutes celles que sa décision laisse passer. Les deux portées se cumulent, "
            + "la plus étroite l'emportant : un champ qui nomme une action n'est demandé que "
            + "par elle.",
            example = "DEMANDER_COMPLEMENT")
    private String actionCode;
}
