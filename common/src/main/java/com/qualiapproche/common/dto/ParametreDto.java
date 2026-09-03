package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Réglage de l'organisation : une clé, une valeur. Les clés dont "
        + "l'application se sert sont semées vides au premier démarrage, pour que "
        + "l'administrateur les trouve dans son écran sans avoir à les deviner ; une valeur vide "
        + "est simplement ignorée par ce qui la lit.")
public class ParametreDto {
    @Schema(description = "Identifiant du réglage. Absent à la création, où le serveur "
            + "l'attribue.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private UUID id;

    /**
     * Identité du réglage. <b>Non modifiable</b> après création : c'est par elle que le code désigne
     * un réglage, et la renommer romprait en silence tout ce qui la lit. Le serveur refuse en 409.
     */
    @Schema(description = "Identité du réglage, en majuscules et soulignés. Normalisée à la "
            + "création, et non modifiable ensuite : c'est par elle que le code désigne un "
            + "réglage, et la renommer romprait en silence tout ce qui la lit. Le serveur refuse "
            + "la tentative en 409 ; un réglage mal nommé se supprime et se recrée.",
            example = "CONTACT_EMAIL")
    private String cle;

    @Schema(description = "Ce que le réglage vaut. Vide, il est ignoré : mieux vaut un pied de "
            + "courriel incomplet qu'un numéro inventé adressé à l'extérieur.",
            example = "contact@exemple.bf")
    private String valeur;

    @Schema(description = "Intitulé du réglage dans l'écran d'administration, à l'usage de qui le "
            + "renseigne et non du code, qui s'en tient à la clé.",
            example = "Courriel de contact")
    private String libelle;

    @Schema(description = "À quoi ce réglage sert et où sa valeur apparaît, pour que "
            + "l'administrateur sache ce qu'il engage en le renseignant.",
            example = "Adresse à laquelle un destinataire de courriel peut répondre ou demander "
                    + "de l'aide.")
    private String description;

    /**
     * {@code TEXTE}, {@code COURRIEL}, {@code TELEPHONE}, {@code URL}, {@code IMAGE},
     * {@code NOMBRE}, {@code ADRESSE}.
     *
     * <p>{@code NOMBRE} manquait à cette liste, alors que {@code TypeParametre} le porte et
     * que l'enregistrement le vérifie : la seule documentation du champ en écartait une
     * valeur pourtant admise.</p>
     */
    @Schema(description = "Nature de la valeur, pour que l'écran sache la présenter et la "
            + "vérifier : un logo se saisit comme une adresse d'image et s'affiche comme telle, "
            + "un nombre est refusé s'il n'en est pas un.",
            example = "COURRIEL",
            allowableValues = {"TEXTE", "COURRIEL", "TELEPHONE", "URL", "IMAGE", "NOMBRE", "ADRESSE"})
    private String type;

    /** Lisible sans habilitation : ce qui figure déjà sur un courriel ou une page publique. */
    @Schema(description = "Le réglage peut-il être lu hors de toute requête utilisateur. Vrai pour "
            + "ce qui figure déjà au bas d'un courriel : le service qui compose ce pied de page "
            + "travaille sur un fil de fond, où aucune permission ne circule. Faux par défaut — un "
            + "réglage n'est pas public parce qu'on a oublié d'y penser.",
            example = "true")
    private boolean lisibleSansHabilitation;
}
