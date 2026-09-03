package com.qualiapproche.common.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Un rôle du realm Keycloak. À ne pas confondre avec les rôles applicatifs, "
        + "tenus en base, qui sont ceux qu'on affecte aux comptes et d'où les permissions sont "
        + "tirées.")
public class KcRoleDto {
    @Schema(description = "Identifiant attribué par Keycloak. Absent à la création.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private String id;

    @Schema(description = "Nom du rôle. C'est par lui, et non par l'identifiant, que le rôle est "
            + "relu, modifié ou supprimé : le renommer revient à en désigner un autre.",
            example = "RESPONSABLE_QUALITE")
    private String name;

    @Schema(description = "Ce que le rôle recouvre, en clair, à l'usage des écrans "
            + "d'administration.",
            example = "Responsable qualité : valide et impute les non-conformités.")
    private String description;

    @Schema(description = "Rôle composé d'autres rôles. La conversion ne le renseigne pas : il "
            + "vaut faux dans toutes les réponses, y compris pour un rôle qui en compose "
            + "d'autres.",
            example = "false")
    private boolean composite;
}
