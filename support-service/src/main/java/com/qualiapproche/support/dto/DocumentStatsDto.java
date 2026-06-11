package com.qualiapproche.support.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * Résumé statistique des documents QMS.
 * Retourné par l'endpoint GET /documents/stats
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentStatsDto {

    /** Nombre total de documents (selon les permissions de l'utilisateur connecté) */
    private long totalDocuments;

    /** Répartition par type de document : {"PRO": 12, "INS": 5, "FOR": 8, ...} */
    private Map<String, Long> countByDocumentType;

    /** Répartition par statut : {"brouillon": 4, "valide": 20, "archive": 3, ...} */
    private Map<String, Long> countByStatus;

    /** Répartition par domaine : {"Qualité": 10, "RH": 6, ...} */
    private Map<String, Long> countByDomaine;

    /** Répartition par service (serviceLibelle) : {"DSI": 8, "DRH": 5, ...} */
    private Map<String, Long> countByService;

    /** Nombre de documents en retard de révision */
    private long documentsEnRetardRevision;

    /** Nombre de documents confidentiels */
    private long documentsConfidentiels;

    /** Nombre de documents externes */
    private long documentsExternes;
}
