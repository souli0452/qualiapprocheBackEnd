package com.qualiapproche.dto;

import com.qualiapproche.entities.CategorieFichier;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FichierDto extends AuditEntityDto{

    private String nomFichier;
    private String descriptionFichier;
    private String typeFichier;
    private String versionFichier;
    private String urlFichier;
    private LocalDateTime dateCreationFichier;
    private LocalDateTime dateModificationFichier;
    private CategorieFichier categorieFichier;
}
