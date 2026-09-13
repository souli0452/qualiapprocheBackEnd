package com.qualiapproche.ia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Réponse à une demande d'assistance.
 *
 * <p>Porte l'identifiant de la trace persistée : c'est lui que le front renvoie au verdict,
 * sans quoi aucune suggestion ne pourrait être rattachée à son sort.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionIaDto {

    /** Texte proposé par l'assistant. */
    private String suggestion;

    /** Identifiant de la trace de la suggestion, à renvoyer au verdict. */
    private UUID suggestionId;

    /** Rappel affiché près du texte : la suggestion engage son vérificateur, pas l'assistant. */
    private String avertissement;
}
