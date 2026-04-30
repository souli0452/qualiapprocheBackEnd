package com.qualiapproche.referentiel.entities;
import com.qualiapproche.common.base.AuditEntity;


import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

import lombok.experimental.SuperBuilder;

@Entity
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@SuperBuilder
public class Demande extends AuditEntity {

    private String libelleDemande;
    private String descriptionDemande;
    private String statutDemande;
   /* @OneToMany
    private List <Fichier> fichier;*/
    private LocalDateTime dateCreationDemande;
    private LocalDateTime dateModificationDemande;
}
