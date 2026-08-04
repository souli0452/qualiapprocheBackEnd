package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Niveau de priorité d'un document, paramétrable par l'organisation.
 *
 * <p>Volontairement ouvert : « Urgent », « Normal », « À traiter avant le 15 » — c'est à la
 * démarche qualité de dire ce qu'elle distingue, non au logiciel.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PrioriteDocumentDto extends AuditEntityDto {
    private String libelle;
    private String description;

    /**
     * Rang d'affichage, du plus urgent au moins urgent. Deux priorités de même rang restent
     * acceptées : l'ordre entre elles n'est alors pas garanti, ce qui ne prête pas à conséquence.
     */
    private Integer ordre;

    /** Couleur d'affichage (ex. {@code #dc2626}), facultative. */
    private String couleur;
}
