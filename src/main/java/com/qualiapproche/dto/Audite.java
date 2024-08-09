package com.qualiapproche.dto;

import com.qualiapproche.entities.NonConformite;
import com.qualiapproche.entities.Risque;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter @Builder
public class Audite extends AuditEntityDto{

    private String libelleAudite;
    private String descriptionAudite;
    private String resultatAudite;
    private String statutAudite;
    private String objectifAudite;
    private String typeAudite;
    private List<Risque> risques;
    private List<NonConformite> nonConformites;
}
