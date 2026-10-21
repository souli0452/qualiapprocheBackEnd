package com.qualiapproche.common.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code PATCH /users/reset-password} : un gestionnaire fixe le mot de passe d'un compte.
 *
 * <p>Le mot de passe voyageait dans l'URL. Une URL se retrouve dans les journaux d'accès de la
 * passerelle et du serveur, dans l'historique du navigateur et dans l'en-tête {@code Referer} :
 * autant d'endroits où un secret n'a rien à faire (CWE-598). Dans le corps, il ne laisse aucune
 * trace en chemin.</p>
 *
 * <p>Les bornes de longueur ne fixent pas la politique de mot de passe — c'est Keycloak qui la
 * porte — elles empêchent seulement qu'on pousse un corps démesuré jusqu'à lui.</p>
 */
@Schema(description = "Mot de passe fixé par un gestionnaire pour un compte.")
public record MotDePasseFixeParAdministrateurDto(

        @Schema(description = "Identifiant Keycloak du compte.", example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
        @NotBlank @Size(max = 64)
        String userId,

        @Schema(description = "Nouveau mot de passe, en clair, transmis à Keycloak puis oublié.")
        @NotBlank @Size(max = 128)
        String password) {

    /** Le mot de passe ne s'écrit jamais dans un journal, même par mégarde. */
    @Override
    public String toString() {
        return "MotDePasseFixeParAdministrateurDto[userId=" + userId + ", password=***]";
    }
}
