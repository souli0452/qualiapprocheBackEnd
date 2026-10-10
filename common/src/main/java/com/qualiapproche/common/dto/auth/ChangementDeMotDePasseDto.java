package com.qualiapproche.common.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code PUT /update-pwd} : l'utilisateur change son mot de passe en présentant l'ancien.
 *
 * <p>Les deux mots de passe passaient dans l'URL (CWE-598). L'ancien est désormais obligatoire :
 * déclaré facultatif, il n'en était pas moins exigé par Keycloak, qui refusait la vérification —
 * l'appel échouait de toute façon, mais sans dire pourquoi.</p>
 */
@Schema(description = "Changement de son propre mot de passe.")
public record ChangementDeMotDePasseDto(

        @Schema(description = "Nom de connexion du compte : celui du jeton, sauf pour un gestionnaire des utilisateurs.",
                example = "a.traore")
        @NotBlank @Size(max = 255)
        String username,

        @Schema(description = "Mot de passe actuel, vérifié auprès de Keycloak.")
        @NotBlank @Size(max = 128)
        String oldPassword,

        @Schema(description = "Nouveau mot de passe.")
        @NotBlank @Size(max = 128)
        String password) {

    /** Aucun des deux mots de passe ne s'écrit dans un journal. */
    @Override
    public String toString() {
        return "ChangementDeMotDePasseDto[username=" + username + ", oldPassword=***, password=***]";
    }
}
