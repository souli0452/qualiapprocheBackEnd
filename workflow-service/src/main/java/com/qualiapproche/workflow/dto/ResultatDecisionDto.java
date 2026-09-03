package com.qualiapproche.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Sort d'un dossier dans une décision groupée.
 *
 * <p>Une décision groupée n'est pas une décision sur N dossiers : ce sont N décisions, dont
 * certaines peuvent échouer. L'appelant peut être habilité sur les uns et pas sur les autres, un
 * dossier peut avoir changé d'étape entre l'affichage de la liste et l'envoi, un champ requis peut
 * manquer ici et pas là. Rendre un simple « c'est fait » laisserait croire à l'utilisateur que tout
 * est passé — c'est précisément ce que faisait l'ancien traitement groupé.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sort d'un seul dossier au sein d'une décision groupée. Une telle décision "
        + "n'est pas une opération sur N dossiers mais N décisions distinctes : la réponse en rend "
        + "autant de lignes, et un appel réussi peut n'avoir rien fait avancer.")
public class ResultatDecisionDto {

    @Schema(description = "Dossier auquel la ligne se rapporte. C'est par lui que l'appelant "
            + "rapproche le sort de chaque dossier de la liste qu'il avait envoyée.")
    private UUID resourceId;

    /** La décision a-t-elle été franchie sur ce dossier ? */
    @Schema(description = "La décision a été franchie sur ce dossier. Faux, le dossier est resté "
            + "où il était : rien n'a été écrit à moitié.",
            example = "false")
    private boolean aboutie;

    /**
     * Ce qui s'y est opposé, en clair et destiné à l'utilisateur : habilitation manquante, étape
     * changée, champ requis absent. Nul lorsque la décision a abouti.
     */
    @Schema(description = "Ce qui s'est opposé à la décision, rédigé pour être montré tel quel : "
            + "habilitation manquante, étape changée depuis l'affichage de la liste, champ requis "
            + "absent. Nul lorsque la décision a abouti.",
            example = "Vous n'avez pas l'habilitation attendue à cette étape.")
    private String motif;
}
