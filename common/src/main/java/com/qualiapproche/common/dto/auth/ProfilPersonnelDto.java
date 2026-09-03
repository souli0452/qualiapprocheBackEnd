package com.qualiapproche.common.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ce qu'un utilisateur peut changer de son propre compte.
 *
 * <p>Volontairement réduit à trois champs. {@code KcUserDto} sert l'administration des comptes et
 * porte aussi l'activation, la structure, les rôles et l'adresse électronique : l'employer ici
 * ouvrirait à chacun le droit de se donner un rôle ou de changer la structure qui commande ses
 * habilitations. Un écran « mon profil » n'a pas à pouvoir cela.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Les trois seules valeurs qu'un utilisateur écrit sur son propre compte. "
        + "L'activation, la structure et les rôles en sont tenus à l'écart à dessein : les y "
        + "admettre laisserait chacun se donner un rôle ou changer la structure d'où il tire ses "
        + "habilitations. L'identifiant du compte modifié n'y figure pas non plus, il est lu dans "
        + "le jeton de l'appelant.")
public class ProfilPersonnelDto {

    @Schema(description = "Prénom. Obligatoire : une valeur vide est refusée.",
            example = "Awa")
    private String firstName;

    @Schema(description = "Nom de famille. Obligatoire : une valeur vide est refusée.",
            example = "Traoré")
    private String lastName;

    @Schema(description = "Numéro de téléphone, facultatif : chiffres, espaces et ponctuation "
            + "usuelle, de six à vingt-cinq caractères, indicatif international admis. Le laisser "
            + "vide retire le numéro au lieu d'y écrire une chaîne vide.",
            example = "+226 70 12 34 56")
    private String phoneNumber;
}
