package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;










@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder

public class DepartementDto extends AuditEntityDto {

    private String libelleDepartement;
    private String descriptionDepartement;

}
