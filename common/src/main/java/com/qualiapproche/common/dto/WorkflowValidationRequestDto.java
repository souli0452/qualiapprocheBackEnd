package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowValidationRequestDto {
    private String comments;
    private Map<Long, String> fields;

    /**
     * Étape sur laquelle l'appelant croit agir ({@code currentStateCode} de l'état qu'il a affiché).
     *
     * <p>Si elle est fournie et ne correspond plus à l'étape réelle, la demande est rejetée en 409 :
     * c'est ce qui empêche un double envoi de franchir deux transitions d'affilée, ou un utilisateur
     * de décider à partir d'un écran périmé. Facultative pour rester compatible avec les appelants
     * qui ne la transmettent pas encore.</p>
     */
    private String expectedStateCode;
}
