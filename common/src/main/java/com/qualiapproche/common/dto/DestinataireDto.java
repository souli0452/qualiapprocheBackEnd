package com.qualiapproche.common.dto;

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
public class DestinataireDto {

    /** Identifiant Keycloak. */
    private String userId;

    private String email;

    /** Nom d'usage, déjà composé ; à défaut, l'adresse sert de libellé. */
    private String nomComplet;
}
