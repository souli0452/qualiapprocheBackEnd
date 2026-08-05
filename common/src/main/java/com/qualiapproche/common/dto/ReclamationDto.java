package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;










@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class ReclamationDto extends AuditEntityDto {
    private String numeroReference;
    private String nomDemendeur;
}
