package com.qualiapproche.dto;

import com.qualiapproche.entities.Audite;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DepartementDto extends AuditEntityDto{

    private String libelleDepartement;
    private String descriptionDepartement;
    private List<Audite> audites;
}
