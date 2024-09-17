package com.qualiapproche.entities;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@Table(name = "fichiers")
public class Fichier extends AuditEntity {
    private String nomFichier;
    private String descriptionFichier;
    private String typeFichier;
    private int versionFichier;
    private String urlFichier;
    private String fichierBase64;
    @OneToMany
    private List<Archivage> archivages;
}
