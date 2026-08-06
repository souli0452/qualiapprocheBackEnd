package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Champ de saisie exigé par une étape de workflow.
 *
 * <p>Exposé dans {@link WorkflowStateDto} pour que l'appelant puisse construire le formulaire
 * de décision : {@code id} est la clé attendue dans {@code WorkflowValidationRequestDto.fields}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepFieldDto {
    private Long id;
    private String fieldName;
    private String fieldLabel;
    private String type;
    private boolean required;

    /**
     * Liste de choix : soit les valeurs séparées par des virgules, soit une source dont elles sont
     * issues ({@code @STRUCTURES}, {@code @UTILISATEURS}).
     */
    private String options;

    /**
     * Décision à laquelle ce champ se rapporte ({@code APPROUVE}, {@code REJETE}), ou {@code null}
     * s'il vaut quelle que soit la décision.
     *
     * <p>Sans cette information, un justificatif de rejet se présentait aussi à qui approuvait :
     * on lui demandait de motiver un refus qu'il n'était pas en train de prononcer. Le champ
     * traverse ce DTO à chaque état de circuit rendu à un module métier — l'omettre ici suffisait
     * à perdre la portée entre le moteur et l'écran.</p>
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
