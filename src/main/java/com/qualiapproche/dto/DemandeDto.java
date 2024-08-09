package com.qualiapproche.dto;

import com.qualiapproche.entities.Fichier;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DemandeDto extends AuditEntityDto{

    private String libelleDemande;
    private String descriptionDemande;
    private String statutDemande;
    private List<Fichier> fichier;
    private LocalDateTime dateCreationDemande;
    private LocalDateTime dateModificationDemande;
}
