package com.qualiapproche.common.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /initiate-reset-pwd} : l'adresse à laquelle envoyer un lien de
 * réinitialisation.
 *
 * <p>L'adresse est une donnée personnelle : passée dans l'URL, elle s'inscrivait dans les journaux
 * d'accès de chaque intermédiaire. La réponse, elle, reste la même que l'adresse ait un compte ou
 * non — seule une adresse mal formée est refusée, ce qui ne dit rien de l'annuaire.</p>
 */
@Schema(description = "Demande d'un lien de réinitialisation du mot de passe.")
public record DemandeDeReinitialisationDto(

        @Schema(description = "Adresse électronique du compte.", example = "a.traore@exemple.bf")
        @NotBlank @Email @Size(max = 254)
        String email) {
}
