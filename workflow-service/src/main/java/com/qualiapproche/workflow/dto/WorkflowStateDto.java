package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStateDto {
    private UUID instanceId;
    private String status;
    private String currentStateCode;
    private String currentStateName;
    private List<WorkflowActionDto> allowedActions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowActionDto {
        private String code;
        private String libelle;
        private String permission;
    }
}
