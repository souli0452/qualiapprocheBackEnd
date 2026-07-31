package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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
    @Builder.Default
    private List<WorkflowActionDto> allowedActions = new ArrayList<>();
}
