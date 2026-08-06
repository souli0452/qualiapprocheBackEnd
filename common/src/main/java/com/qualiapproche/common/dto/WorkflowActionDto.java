package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowActionDto {
    /** Identifiant de la transition à jouer — c'est lui que reprend {@code /execute}. */
    private String code;
    /**
     * Code métier de l'action au sein de son étape ({@code APPROUVE}, {@code DEMANDER_COMPLEMENT}…),
     * stable d'une installation à l'autre là où {@link #code} est un identifiant technique. C'est
     * par lui qu'un champ se rattache à une action précise.
     */
    private String actionCode;
    private String libelle;
    /**
     * Icône du bouton, en classe PrimeIcons ({@code "pi pi-check"}), telle que le circuit la
     * déclare. Absente de ce DTO, elle était publiée par le moteur et perdue avant l'écran, qui
     * retombait sur une présentation uniforme.
     */
    private String icon;
    /**
     * Couleur du bouton dans le vocabulaire PrimeNG ({@code success}, {@code warn}, {@code danger}…),
     * utilisable telle quelle. Même remarque que pour l'icône.
     */
    private String severity;
    private String permission;
    /**
     * Décision portée par la transition ({@code APPROUVE} / {@code REJETE}). Permet à l'appelant
     * de distinguer une action d'approbation d'un rejet — pour la présentation comme pour choisir
     * entre les points d'entrée {@code /validate} et {@code /reject}.
     */
    private String decision;
}
