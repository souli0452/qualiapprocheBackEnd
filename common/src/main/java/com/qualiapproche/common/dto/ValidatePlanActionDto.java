package com.qualiapproche.common.dto;
import lombok.*;
import java.util.List;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidatePlanActionDto {
    private UUID nonConformiteId;
    private List<UUID> planIds;
}
