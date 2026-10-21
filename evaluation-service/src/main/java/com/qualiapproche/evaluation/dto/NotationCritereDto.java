package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.evaluation.enumeration.ConstatRQAPBF;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.enumeration.StatutNotation;
import jakarta.validation.constraints.NotNull;
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
public class NotationCritereDto {
    private UUID id;
    /** Renseignés dans la liste de tous les écarts (D18), qui mêle les audits. */
    private UUID auditId;
    private String auditReference;
    @NotNull
    private UUID noeudId;
    private String noeudCode;
    private String noeudLibelle;
    private String preuveAttendue;
    @NotNull
    private NiveauNotationRQAPBF niveau;
    private Double score;
    private String preuve;
    private String commentaire;
    private ConstatRQAPBF constatRetenu;
    private StatutNotation statut;
    private UUID nonConformiteId;
    /** Critère publié au constat NC dont la non-conformité n'a pas encore pu être ouverte. */
    private boolean transmissionEnAttente;
}
