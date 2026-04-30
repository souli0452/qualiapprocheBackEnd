package com.qualiapproche.common.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class EvaluationDto extends AuditEntityDto {
    private String libelle;
    private String description;
}
