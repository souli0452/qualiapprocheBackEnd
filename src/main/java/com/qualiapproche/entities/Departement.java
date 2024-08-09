package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Departement extends AuditEntity {

    private String libelleDepartement;
    private String descriptionDepartement;
    @ManyToMany
    private List<Audite> audites;
}
