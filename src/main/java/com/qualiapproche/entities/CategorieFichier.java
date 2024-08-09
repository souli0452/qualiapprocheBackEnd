package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CategorieFichier extends AuditEntity {

    private String libelleCategorie;
    private String descriptionCategorie;
    private String necessiteDemandeCreationFichier;
    private LocalDateTime dateCreationCategorie;
    private LocalDateTime dateModificationCategorie;
    @OneToMany
    private List<Fichier> fichiers;
}
