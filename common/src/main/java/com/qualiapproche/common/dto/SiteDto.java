package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Un lieu de l'organisation : là où se déroule un audit, et demain d'autres activités.
 *
 * <p>Un site retiré n'est pas effacé : il reste lisible par les dossiers qui le citent, mais ne
 * se propose plus dans les listes de choix.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Un lieu de l'organisation, où se déroulent notamment les audits.")
public class SiteDto extends AuditEntityDto {

    @Schema(description = "Nom du site.", example = "Siège — Ouagadougou")
    private String nom;

    @Schema(description = "Adresse du site.", example = "Avenue Kwame Nkrumah")
    private String adresse;

    @Schema(description = "Faux une fois le site retiré : il ne se propose plus dans les choix.")
    private boolean actif;
}
