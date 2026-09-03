package com.qualiapproche.common.dto.auth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/11/25 à 15:15
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "La réponse de Keycloak à une demande de jeton, telle qu'elle arrive : les "
        + "propriétés sont nommées à sa façon, en minuscules séparées par des tirets bas. Objet "
        + "interne au serveur — les deux jetons qu'il porte sont déposés en cookies HTTP-Only et "
        + "ne sortent jamais par le corps d'une réponse.")
public class KcTokenDto {
    @Schema(description = "Jeton d'accès, présenté à chaque appel. Déposé en cookie, jamais rendu "
            + "à l'appelant.",
            example = "xxxxx.yyyyy.zzzzz")
    private String accessToken;

    @Schema(description = "Jeton de renouvellement, qui sert à obtenir un nouvel accès et que la "
            + "déconnexion révoque auprès de Keycloak. Déposé en cookie, lui aussi.",
            example = "xxxxx.yyyyy.zzzzz")
    private String refreshToken;

    @Schema(description = "Durée de validité du jeton d'accès, en secondes.",
            example = "300")
    private Integer expiresIn;

    @Schema(description = "Durée de validité du jeton de renouvellement, en secondes. C'est elle "
            + "qui borne la session : passé ce délai, il faut se reconnecter.",
            example = "1800")
    private Integer refreshExpiresIn;

    @Schema(description = "Manière de présenter le jeton d'accès dans l'en-tête d'autorisation.",
            example = "Bearer")
    private String tokenType;

    @Schema(description = "Session ouverte du côté de Keycloak, que la révocation ferme.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private String sessionState;

    @Schema(description = "Portées accordées au jeton, séparées par des espaces.",
            example = "openid profile email")
    private String scope;
}
