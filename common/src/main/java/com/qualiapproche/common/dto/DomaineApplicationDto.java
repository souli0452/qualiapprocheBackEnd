package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Domaine d'application d'un document — le champ d'activité qu'il couvre.
 *
 * <p>C'était une saisie libre : chaque rédacteur écrivait « RH », « Ressources Humaines » ou
 * « ressources humaines », et le regroupement statistique par domaine comptait trois domaines là
 * où il n'y en avait qu'un.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class DomaineApplicationDto extends AuditEntityDto {
    private String libelle;
    private String description;
    private Integer ordre;
}
