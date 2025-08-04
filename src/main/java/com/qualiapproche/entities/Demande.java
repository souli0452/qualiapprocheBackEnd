package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Demande extends AuditEntity {

    private String libelleDemande;
    private String descriptionDemande;
    private String statutDemande;
    @OneToMany
    private List <Fichier> fichier;
    private LocalDateTime dateCreationDemande;
    private LocalDateTime dateModificationDemande;
}
