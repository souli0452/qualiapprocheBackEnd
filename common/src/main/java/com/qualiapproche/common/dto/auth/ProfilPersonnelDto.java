package com.qualiapproche.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ce qu'un utilisateur peut changer de son propre compte.
 *
 * <p>Volontairement réduit à trois champs. {@code KcUserDto} sert l'administration des comptes et
 * porte aussi l'activation, la structure, les rôles et l'adresse électronique : l'employer ici
 * ouvrirait à chacun le droit de se donner un rôle ou de changer la structure qui commande ses
 * habilitations. Un écran « mon profil » n'a pas à pouvoir cela.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilPersonnelDto {

    private String firstName;
    private String lastName;
    private String phoneNumber;
}
