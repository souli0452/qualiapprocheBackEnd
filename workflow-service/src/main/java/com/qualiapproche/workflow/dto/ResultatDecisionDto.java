package com.qualiapproche.workflow.dto;

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
public class ResultatDecisionDto {

    private UUID resourceId;

    /** La décision a-t-elle été franchie sur ce dossier ? */
    private boolean aboutie;

    /**
     * Ce qui s'y est opposé, en clair et destiné à l'utilisateur : habilitation manquante, étape
     * changée, champ requis absent. Nul lorsque la décision a abouti.
     */
    private String motif;
}
