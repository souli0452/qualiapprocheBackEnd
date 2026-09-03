package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;






@Data
@NoArgsConstructor
@AllArgsConstructor


@Schema(description = "L'état d'un compte au regard de la connexion : ce qui empêche, ou non, son "
        + "titulaire d'entrer normalement. Aucun point d'entrée ne le renvoie aujourd'hui.")
public class UserStatusDto {
    @Schema(description = "Adresse électronique confirmée par son titulaire. C'est cette "
            + "confirmation qui rend définitif le mot de passe engendré à la création du compte.",
            example = "true")
    private boolean emailVerified;

    @Schema(description = "Compte actif. Un compte désactivé libère une place au regard du "
            + "plafond de la licence, et le réactiver en reprend une.",
            example = "true")
    private boolean enabled;

    @Schema(description = "Mot de passe encore temporaire : la connexion exigera d'en choisir un "
            + "autre avant toute autre chose.",
            example = "false")
    private boolean temporaryPwd;
}
