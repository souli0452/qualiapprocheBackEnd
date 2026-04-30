package com.qualiapproche.referentiel.entities;
import com.qualiapproche.common.base.AuditEntity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import lombok.experimental.SuperBuilder;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class CategorieFichier extends AuditEntity {

    private String libelleCategorie;
    private String descriptionCategorie;
    private String necessiteDemandeCreationFichier;

   /* @OneToMany(mappedBy = "categorieFichier")
    private List<Fichier> fichiers;*/
}
