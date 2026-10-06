package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ConstatDto {
    private UUID id;
    private UUID auditId;
    private String critereAudit;
    @NotBlank
    private String observation;
    private StatutConstat statut;
    private LocalDateTime datePublication;
    private LocalDateTime dateSaisie;
    private String commentaireEquipe;
    private UUID natureId;
    private String natureLibelle;
    private String natureCouleur;
    private boolean nonConformite;
    private UUID checklistId;
    private UUID pointControleId;
    private UUID redacteurId;
    private String redacteurUtilisateurId;
    private UUID nonConformiteId;
    /** Écart publié dont la non-conformité n'a pas encore pu être ouverte : à reprendre depuis l'audit. */
    private boolean transmissionEnAttente;
}
