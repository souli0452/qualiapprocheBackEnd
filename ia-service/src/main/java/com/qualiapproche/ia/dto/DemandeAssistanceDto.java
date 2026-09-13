package com.qualiapproche.ia.dto;

import com.qualiapproche.ia.enumeration.TypeAssistance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Demande d'assistance rédactionnelle.
 *
 * <p>Le {@code contexte} est une map libre (processus, origine, niveau...) plutôt que des champs
 * nommés : chaque type d'assistance attend un contexte différent, et le figer dans la signature
 * imposerait une évolution de l'API à chaque nouveau type.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeAssistanceDto {

    @NotNull(message = "Le type d'assistance est obligatoire.")
    private TypeAssistance typeAssistance;

    /** Brouillon ou notes de l'utilisateur, matière première de la suggestion. */
    @NotBlank(message = "Le texte source est obligatoire.")
    private String texteSource;

    /** Éléments de contexte connus de l'écran appelant (processus, origine, niveau...). */
    private Map<String, String> contexte;

    /** Type de la ressource métier concernée, pour la traçabilité (facultatif). */
    private String ressourceType;

    /** Identifiant de la ressource métier concernée, pour la traçabilité (facultatif). */
    private UUID ressourceId;
}
