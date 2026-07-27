package com.qualiapproche.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepDto{
    private Long id;
    private String nomEtape;
    private int stepOrder;
    private String responsableRole;
    private String description;

    /** Entrée du catalogue d'étapes réutilisables dont cette étape a été instanciée (voir WorkflowStepTemplate). */
    private UUID stepTemplateId;

    /**
     * Transitions sortantes explicites de cette étape (une par décision).
     * Optionnel : si absent ou incomplet, le circuit applique par défaut la logique séquentielle
     * historique (étape suivante/précédente par stepOrder) pour les décisions non fournies,
     * sans jamais écraser une transition déjà configurée par ailleurs.
     */
    private List<WorkflowTransitionDto> transitions;
}
