package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Utilisateur joignable, tel que le résout user-service à partir d'un rôle applicatif.
 *
 * <p>Réduit à ce qu'exige l'envoi d'une notification : à qui écrire, et sous quel nom
 * l'interpeller. Les autres attributs du profil — structure, licence, permissions — ne
 * regardent pas les services qui notifient.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Utilisateur joignable, réduit à ce qu'exige l'envoi d'une notification : à "
        + "qui écrire et sous quel nom l'interpeller. Structure, licence et permissions ne "
        + "regardent pas les services qui notifient, et n'y figurent donc pas.")
public class DestinataireDto {

    /** Identifiant Keycloak. */
    @Schema(description = "Identifiant de l'utilisateur chez Keycloak, qui tient les comptes. "
            + "C'est lui que le registre des notifications conserve, l'adresse pouvant changer.",
            example = "3f2a6c18-7b4d-4e91-9a02-5c8de1f7b430")
    private String userId;

    @Schema(description = "Adresse à laquelle le message part.", example = "a.traore@exemple.bf")
    private String email;

    /** Nom d'usage, déjà composé ; à défaut, l'adresse sert de libellé. */
    @Schema(description = "Nom d'usage, déjà composé par le service des comptes : rien ici n'a à "
            + "recoller un prénom et un nom. À défaut, l'adresse tient lieu de libellé.",
            example = "Awa Traoré")
    private String nomComplet;
}
