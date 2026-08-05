package com.qualiapproche.referentiel.entities;
import com.qualiapproche.common.base.AuditEntity;


import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

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
