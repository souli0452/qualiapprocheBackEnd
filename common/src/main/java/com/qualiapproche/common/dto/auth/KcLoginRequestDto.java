package com.qualiapproche.common.dto.auth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;





/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/11/25 à 15:01
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Schema(description = "Ce qu'un utilisateur présente pour ouvrir sa session. Les identifiants "
        + "partent tels quels vers Keycloak et ne sont conservés nulle part ici ; les jetons "
        + "obtenus reviennent en cookies HTTP-Only et non dans le corps de la réponse.")
public class KcLoginRequestDto {
    @Schema(description = "Nom de connexion du compte Keycloak, qui n'est pas nécessairement "
            + "l'adresse électronique.",
            example = "a.traore")
    private String username;

    @Schema(description = "Mot de passe en clair, transmis à Keycloak puis oublié.",
            example = "exemple")
    private String password;

    @Schema(description = "Sans emploi à l'ouverture de session : le renouvellement lit le jeton "
            + "dans le cookie et non dans ce corps. Le renseigner ne produit aucun effet.",
            example = "inutilise")
    private String refreshToken;
}
