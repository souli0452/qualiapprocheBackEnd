package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowActionDto {
    private String code;
    private String libelle;
    private String permission;
    /**
     * Décision portée par la transition ({@code APPROUVE} / {@code REJETE}). Permet à l'appelant
     * de distinguer une action d'approbation d'un rejet — pour la présentation comme pour choisir
     * entre les points d'entrée {@code /validate} et {@code /reject}.
     */
    private String decision;
}
