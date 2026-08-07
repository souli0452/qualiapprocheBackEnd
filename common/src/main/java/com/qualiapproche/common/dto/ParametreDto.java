package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Réglage de l'organisation : une clé, une valeur.
 *
 * <p>Dans {@code common} et non dans le service : les autres services les lisent — le pied de page
 * des courriels vient de là — et leur client Feign a besoin de cette forme.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametreDto {
    private UUID id;

    /**
     * Identité du réglage. <b>Non modifiable</b> après création : c'est par elle que le code désigne
     * un réglage, et la renommer romprait en silence tout ce qui la lit. Le serveur refuse en 409.
     */
    private String cle;

    private String valeur;
    private String libelle;
    private String description;

    /** {@code TEXTE}, {@code COURRIEL}, {@code TELEPHONE}, {@code URL}, {@code IMAGE}, {@code ADRESSE}. */
    private String type;

    /** Lisible sans habilitation : ce qui figure déjà sur un courriel ou une page publique. */
    private boolean lisibleSansHabilitation;
}
