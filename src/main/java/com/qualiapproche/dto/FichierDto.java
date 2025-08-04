package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
public class FichierDto extends AuditEntityDto{

    private String nomFichier;
    private String descriptionFichier;
    private String typeFichier;
    private int versionFichier;
    private String urlFichier;
    private String fichierBase64;
}
