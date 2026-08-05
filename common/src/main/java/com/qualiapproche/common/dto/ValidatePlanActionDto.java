package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
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
