package com.qualiapproche.evaluation.dto;

import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutPlanAudit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Où en est la réalisation d'un audit (C9) : plan, constats, RQAP-BF, et ce qui retient la clôture. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvancementDto {

    private StatutAudit statut;

    /** Nul tant que l'audit n'a pas de plan. */
    private StatutPlanAudit statutPlan;
    private boolean planPartage;

    /** Points de contrôle des checklists de l'audit. */
    private long pointsDeControle;
    /** Points sur lesquels au moins un constat a été relevé, quel que soit son palier. */
    private long pointsRenseignes;
    private Double pourcentageConstats;
    private long constatsBrouillon;
    private long constatsCompiles;
    private long constatsPublies;

    /** Critères RQAP-BF à évaluer : ceux des domaines de l'audit, ou tout le référentiel sans domaine. */
    private long criteresAEvaluer;
    private long criteresNotes;
    private long criteresPublies;
    private Double pourcentageRqapbf;

    /**
     * Écarts publiés — constats et critères RQAP-BF — qui n'ont pas encore de dossier au module
     * Non-conformités : la fiche propose de reprendre leur transmission tant qu'il en reste.
     */
    private long ecartsEnAttente;

    private boolean cloturable;
    private List<String> raisonsDeNePasCloturer;
}
