package com.qualiapproche.evaluation.dto;

import com.qualiapproche.evaluation.enumeration.NiveauRisque;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Les filtres du programme annuel (A2) et du suivi (D16). Tous facultatifs : un filtre absent
 * ne restreint rien.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiltreProgramme {
    private List<StatutAudit> statuts;
    private String recherche;
    private UUID typeAuditId;
    /** Un domaine RQAP-BF concerné par l'audit. */
    private UUID domaineId;
    private NiveauRisque niveauRisque;
    private UUID responsableId;
    /** Vrai : les seuls audits en retard ; faux : les seuls à l'heure. */
    private Boolean enRetard;
    /** Les seuls audits dont l'appelant est membre de l'équipe, responsable compris (« Mes audits »). */
    private boolean mesAudits;
    /** Les audits dont la date de début prévue tombe dans cette année. */
    private Integer annee;
}
