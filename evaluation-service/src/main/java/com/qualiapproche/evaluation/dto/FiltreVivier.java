package com.qualiapproche.evaluation.dto;

import com.qualiapproche.evaluation.enumeration.StatutAuditeur;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Les filtres du portefeuille des auditeurs (A4). Tous facultatifs : un filtre absent ne
 * restreint rien.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiltreVivier {
    /** Les personnes que la recherche par nom désigne, résolues dans l'annuaire par l'écran. */
    private List<String> utilisateurIds;
    private StatutAuditeur statut;
    private UUID structureId;
    /** Une certification détenue, sans tenir compte de la casse. */
    private String certification;
    /**
     * La tranche de moyenne d'évaluation qui correspond à un niveau de l'échelle : bornes
     * calculées par l'écran, qui seul connaît l'échelle paramétrée. Le minimum est exclu et le
     * maximum compris : une moyenne à mi-chemin de deux niveaux va au plus bas, comme à l'affichage.
     */
    private Double moyenneMin;
    private Double moyenneMax;
    /** Vrai : les seuls auditeurs qui n'ont encore reçu aucune évaluation. */
    private boolean nonEvalues;
}
