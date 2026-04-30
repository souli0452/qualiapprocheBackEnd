package com.qualiapproche.common.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class DemandeDto extends AuditEntityDto {
    private String libelleDemande;
    private String descriptionDemande;
    private String statutDemande;
}
