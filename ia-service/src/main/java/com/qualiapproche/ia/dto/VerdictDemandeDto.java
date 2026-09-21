package com.qualiapproche.ia.dto;

import com.qualiapproche.ia.enumeration.VerdictSuggestion;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Sort que l'utilisateur réserve à une suggestion de l'assistant. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerdictDemandeDto {

    @NotNull(message = "Le verdict est obligatoire.")
    private VerdictSuggestion verdict;
}
