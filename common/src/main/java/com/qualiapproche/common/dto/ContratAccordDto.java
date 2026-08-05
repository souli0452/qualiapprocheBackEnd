package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContratAccordDto extends AuditEntityDto {
    private String libelleContratAccord;
    private String descriptionContratAccord;
    private java.time.LocalDateTime dateSignature;
    private UUID prestataireId;
}
