package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Fichier extends AuditEntity {
    private String nomFichier;
    private String descriptionFichier;
    private String typeFichier;
    private String versionFichier;
    private String urlFichier;
    private LocalDateTime dateCreationFichier;
    private LocalDateTime dateModificationFichier;
    @OneToMany
    private List<Archivage> archivages;
    @OneToOne
    private CategorieFichier categorieFichier;

}
