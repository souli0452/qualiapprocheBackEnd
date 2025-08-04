package com.qualiapproche.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidatePlanActionDto {
    private UUID nonConformiteId;
    private List<UUID> planIds;
}
