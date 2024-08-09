package com.qualiapproche.entities;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Reglementation extends AuditEntity {

    private String nomReglementation;
    private String descriptionReglementation;
    private String OrganismeReglementation;
    @ManyToMany
    private List<Exigence> exigences;
    @ManyToMany
    private List<SuiviAuditInspection> suiviAuditInspections;

}
