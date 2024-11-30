package com.qualiapproche.dto;

import com.qualiapproche.entities.Fichier;
import com.qualiapproche.entities.Reclamation;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import jakarta.persistence.MappedSuperclass;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
public class NonConformiteDto extends AuditEntityDto {

    private String intitule;
    private String typeNonConformite;
    private String numeroReference;
    private String priorite;
    private String detailleSuplementaire;
    private String dateEcheance;
    private String statut;
    private String commentaires;
    private Reclamation reclamation;
    private List<Fichier> fichiers;
}
