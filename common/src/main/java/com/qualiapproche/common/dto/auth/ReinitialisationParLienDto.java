package com.qualiapproche.common.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code PUT /reinitialize-pwd} : le nouveau mot de passe, sur présentation du jeton reçu
 * par courriel.
 *
 * <p>Jeton et mot de passe passaient dans l'URL, où ils survivaient dans les journaux d'accès et
 * l'historique du navigateur (CWE-598) : quiconque relisait un journal pouvait rejouer le lien
 * tant qu'il n'avait pas servi. Le lien du courriel, lui, porte toujours le jeton — c'est l'écran
 * qui le reprend et le place dans ce corps.</p>
 */
@Schema(description = "Réinitialisation d'un mot de passe par le lien reçu par courriel.")
public record ReinitialisationParLienDto(

        @Schema(description = "Identifiant Keycloak du compte, repris du lien.")
        @NotBlank @Size(max = 64)
        String userId,

        @Schema(description = "Jeton du lien, tel que reçu dans le courriel.")
        @NotBlank @Size(max = 128)
        String token,

        @Schema(description = "Nouveau mot de passe, en clair, transmis à Keycloak puis oublié.")
        @NotBlank @Size(max = 128)
        String password) {

    /** Ni le jeton ni le mot de passe ne s'écrivent dans un journal. */
    @Override
    public String toString() {
        return "ReinitialisationParLienDto[userId=" + userId + ", token=***, password=***]";
    }
}
