package com.qualiapproche.common.dto;

import com.qualiapproche.common.enumeration.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NcDashboardDto {
    private long totalNC;
    private Map<Status, Long> statsByStatus;
    private Map<Status, Map<String, Long>> statsByStatusAndGravity;
}
