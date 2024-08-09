package com.qualiapproche.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Reclamation extends AuditEntity {

    private String numeroReference;
    private String nomDemendeur;
    private String dateReclamation;
    @OneToMany
    private List<Reclamation> reclamations;
    @OneToMany
    private List<ActionCorrectivePreventive> actionCorrectivePreventives;
    private LocalDateTime dateModificationReclamation;
}
