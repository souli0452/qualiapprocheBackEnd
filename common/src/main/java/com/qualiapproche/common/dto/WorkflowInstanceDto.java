package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Vue minimale d'une instance de validation de workflow renvoyée par workflow-service
 * (initiation, dernière instance connue pour une ressource).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceDto {
    private UUID instanceId;
    private UUID workflowId;
    private String status;
    private String currentStateCode;
    private String currentStateName;
}
