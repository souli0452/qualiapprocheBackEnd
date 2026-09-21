package com.qualiapproche.ia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un chiffre, déjà mis en forme par le serveur.
 *
 * <p>La valeur voyage en texte, prête à afficher : ni l'écran ni — surtout — le modèle n'ont à la
 * recalculer ou à l'arrondir. C'est la même règle que pour les listes : ce qui se compte vient du
 * module métier et s'affiche tel quel ; l'assistant commente à côté.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChiffreDto {

    private String libelle;
    private String valeur;

    /** Nuance d'alerte : {@code null} si le chiffre n'appelle aucune attention particulière. */
    private String alerte;
}
