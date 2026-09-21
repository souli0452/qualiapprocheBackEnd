package com.qualiapproche.ia.dto;

import com.qualiapproche.ia.enumeration.TypeAssistance;
import com.qualiapproche.ia.enumeration.VerdictSuggestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une trace de suggestion, telle que l'historique la rend.
 *
 * <p>Le texte source et le contexte n'y figurent pas : l'historique sert à suivre le sort des
 * suggestions, et les porter doublerait en réponse des contenus déjà lus à l'écran d'origine.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionHistoriqueDto {

    private UUID id;
    private TypeAssistance typeAssistance;
    private String promptVersion;
    private String modele;
    private String suggestion;
    private VerdictSuggestion verdict;
    private String ressourceType;
    private UUID ressourceId;
    private Long dureeMs;
    private LocalDateTime createdAt;
}
