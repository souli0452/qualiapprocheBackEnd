package com.qualiapproche.support.dto;

import com.qualiapproche.support.model.StepDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transition explicite pour une étape : pour la {@code decision} donnée, quelle est l'étape
 * cible (référencée par son {@code stepOrder} au sein du même workflow) ?
 * {@code toStepOrder == null} = fin de circuit (validation si APPROUVE, retour brouillon si REJETE).
 * Si {@code requiredRole} n'est pas fourni, le rôle requis retombe sur celui de l'étape source.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransitionDto {
    private Long id;
    private StepDecision decision;
    private Integer toStepOrder;
    private String requiredRole;
    private String label;
}