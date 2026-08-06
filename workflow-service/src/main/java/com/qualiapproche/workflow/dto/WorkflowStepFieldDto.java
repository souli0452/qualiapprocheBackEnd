package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepFieldDto {
    private Long id;
    private String fieldName;
    private String fieldLabel;
    private String type; // string, numeric, select, file, date
    private boolean required;
    private String options;

    /**
     * Décision à laquelle le champ se rapporte ({@code APPROUVE}, {@code REJETE}), ou {@code null}
     * s'il vaut quelle que soit la décision. L'écran n'a ainsi à présenter que ce que la décision
     * choisie réclame.
     */
    private String decision;

    /**
     * Code de l'action qui, seule, réclame ce champ — ou {@code null} s'il vaut pour toutes celles
     * que sa décision laisse passer.
     *
     * <p>Nécessaire dès qu'une étape offre plusieurs actions de même nature : sans lui, le motif
     * demandé par « Demander un complément » se présenterait aussi à qui valide simplement.</p>
     */
    private String actionCode;
}
