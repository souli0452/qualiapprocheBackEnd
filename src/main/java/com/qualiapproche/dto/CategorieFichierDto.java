package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.entities.Fichier;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder

public class CategorieFichierDto extends AuditEntityDto{

    private String libelleCategorie;
    private String descriptionCategorie;
    private String necessiteDemandeCreationFichier;
    private List<Fichier> fichiers;
}
