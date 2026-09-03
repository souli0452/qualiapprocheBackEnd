package com.qualiapproche.common.dto.auth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;





import java.util.List;

/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/11/26 à 14:29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Un compte tel que l'administration des comptes le manipule : même objet "
        + "pour la lecture, la création et la modification, d'où des champs sans emploi dans "
        + "l'un ou l'autre sens. Un écran « mon profil » ne doit pas s'en servir — il passe par "
        + "ProfilPersonnelDto, faute de quoi un formulaire réduit désactiverait le compte et "
        + "effacerait ses rôles.")
public class KcUserDto {
    @Schema(description = "Identifiant du compte dans Keycloak. Absent à la création, où "
            + "Keycloak l'attribue ; exigé à la modification.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private String id;

    @Schema(description = "Date de création du compte en millisecondes depuis le 1er janvier "
            + "1970, telle que Keycloak la tient. Posée par lui.",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "1732541460000")
    private Long createdTimestamp;

    @Schema(description = "Nom de connexion. La modification ne le reprend pas : il reste celui "
            + "de la création.",
            example = "a.traore")
    private String username;

    @Schema(description = "Compte actif. À la création la valeur envoyée est ignorée, le compte "
            + "naît actif ; à la modification elle est appliquée telle quelle, et un formulaire "
            + "qui l'omet désactive le compte.",
            example = "true")
    private boolean enabled;

    @Schema(description = "Adresse confirmée par son titulaire. À la création, c'est elle qui "
            + "décide du mot de passe engendré : à faux, il n'est que temporaire et devra être "
            + "changé à la première connexion.",
            example = "true")
    private boolean emailVerified;

    @Schema(description = "Prénom, qui paraîtra sur les dossiers que l'agent vise.",
            example = "Awa")
    private String firstName;

    @Schema(description = "Identifiant de la structure de rattachement, rangé dans un attribut "
            + "Keycloak. C'est lui qui borne ce que l'agent voit et ce qu'il peut décider : le "
            + "changer déplace ses habilitations avec lui.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private String structure;

    @Schema(description = "Nom de famille, qui suit le prénom dans le nom affiché.",
            example = "Traoré")
    private String lastName;

    @Schema(description = "Adresse électronique. Elle reçoit le mot de passe engendré à la "
            + "création, puis les avis d'étape des circuits.",
            example = "a.traore@exemple.bf")
    private String email;

    @Schema(description = "Sans emploi. Le serveur engendre lui-même le mot de passe à la "
            + "création et l'envoie par courriel ; aucune réponse ne le renvoie jamais, et le "
            + "changement de mot de passe a ses propres points d'entrée.",
            example = "inutilise")
    private String password;

    @Schema(description = "Fonction occupée dans la structure, rangée dans un attribut Keycloak. "
            + "Sert à l'affichage et n'accorde aucun droit.",
            example = "Chef de service")
    private String fonction;

    @Schema(description = "Numéro de téléphone, rangé dans un attribut Keycloak.",
            example = "+226 70 12 34 56")
    private String phoneNumber;

    @Schema(description = "Noms des rôles applicatifs du compte. La modification les remplace en "
            + "bloc : les affectations existantes sont effacées d'abord, si bien qu'une liste "
            + "absente les retire toutes au lieu de les laisser en place.")
    private List<String> roles;

}
