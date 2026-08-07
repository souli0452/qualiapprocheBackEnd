package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDto {
    private UUID id;
    private String nom;
    private String description;
    private String resourceType;
    @Builder.Default
    private boolean actif = true;
    /**
     * Entité à laquelle le circuit est réservé au sein de sa famille — l'identifiant d'un type de
     * document, par exemple — ou vide s'il est le circuit par défaut de la famille.
     *
     * <p>Vide et {@code null} valent la même chose : un champ effacé dans l'éditeur arrive en chaîne
     * vide, et l'écran ne doit pas avoir à connaître cette nuance pour rendre un circuit à la
     * famille entière.</p>
     */
    private String cibleId;
    @Builder.Default
    private List<WorkflowStepDto> steps = new ArrayList<>();
}
