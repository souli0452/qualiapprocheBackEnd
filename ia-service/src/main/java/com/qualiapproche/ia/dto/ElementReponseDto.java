package com.qualiapproche.ia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Une ligne de résultat, rendue <b>telle quelle</b> par l'écran.
 *
 * <p>Ces champs ne passent jamais par la plume du modèle : c'est la règle qui protège l'exactitude.
 * Un assistant qui réécrirait « quatre dossiers » là où il y en a trois ne commettrait pas une
 * maladresse de style — il produirait un enregistrement faux dans un système qualité.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementReponseDto {

    /** Numéro du dossier, tel qu'il s'affiche partout ailleurs. */
    private String reference;

    /** De quoi reconnaître le dossier en une ligne. */
    private String libelle;

    /** Niveau de gravité, s'il est renseigné. */
    private String niveau;

    /** Où en est le dossier. */
    private String etat;

    /** Type et identifiant de la ressource, pour que l'écran puisse y conduire. */
    private String ressourceType;
    private String ressourceId;
}
