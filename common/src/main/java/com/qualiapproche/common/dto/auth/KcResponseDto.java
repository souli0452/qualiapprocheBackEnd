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
 * @since : 2024/11/25 à 14:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "La réponse brève des points d'entrée d'authentification. C'est aussi la "
        + "forme que prennent les refus du filtre de sécurité, connexion absente comme droit "
        + "manquant : elle ne suit donc pas l'enveloppe des réponses métier.")
public class KcResponseDto {
    @Schema(description = "Issue de l'appel, en majuscules. Les valeurs sont posées par l'appelé "
            + "au cas par cas plutôt que tirées d'une liste fermée.",
            example = "ACCESS_DENIED")
    private String status;

    @Schema(description = "Phrase destinée à l'utilisateur, en français. Absente quand l'issue se "
            + "suffit à elle-même.",
            example = "Vous devez vous connecter pour accéder à cette ressource")
    private String message;

    @Schema(description = "Charge utile éventuelle, de forme libre selon le point d'entrée. Nulle "
            + "dans les réponses de refus.")
    private Object data;
}
