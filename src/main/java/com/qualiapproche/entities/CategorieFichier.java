package com.qualiapproche.entities;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
public class CategorieFichier extends AuditEntity {

    private String libelleCategorie;
    private String descriptionCategorie;
    private String necessiteDemandeCreationFichier;
    @OneToMany
    private List<Fichier> fichiers;
}
