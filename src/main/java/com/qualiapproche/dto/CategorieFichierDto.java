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
public class CategorieFichierDto extends AuditEntityDto{

    private String libelleCategorie;
    private String descriptionCategorie;
    private String necessiteDemandeCreationFichier;
    private LocalDateTime dateCreationCategorie;
    private LocalDateTime dateModificationCategorie;
    private List<Fichier> fichiers;
}
