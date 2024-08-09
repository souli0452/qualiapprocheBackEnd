package com.qualiapproche.dto;

import com.qualiapproche.entities.Fichier;
import com.qualiapproche.entities.Reclamation;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class NonConformiteDto extends AuditEntityDto {

    private String intitule;
    private String typeNonConformite;
    private String numeroReference;
    private String priorite;
    private String detailleSuplementaire;
    private LocalDateTime dateEcheance;
    private String statut;
    private String commentaires;
    private Reclamation reclamation;
    private List<Fichier> fichiers;
}
