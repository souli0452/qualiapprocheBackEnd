package com.qualiapproche.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * Résumé statistique des documents QMS.
 * Retourné par l'endpoint GET /documents/stats
 */
@Schema(description = "Tableau de bord documentaire. Les chiffres ne portent que sur les documents "
        + "que l'appelant a le droit de voir : deux personnes de structures différentes n'y liront "
        + "pas les mêmes totaux.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentStatsDto {

    /** Nombre total de documents (selon les permissions de l'utilisateur connecté) */
    @Schema(description = "Effectif de référence auquel se rapportent les répartitions ci-dessous.",
            example = "148")
    private long totalDocuments;

    /** Répartition par type de document : {"PRO": 12, "INS": 5, "FOR": 8, ...} */
    @Schema(description = "Compte par code de type documentaire. Les types sans document ne "
            + "figurent pas dans la carte : l'écran doit prévoir les absences.",
            example = "{\"PRO\": 12, \"INS\": 5, \"FOR\": 8}")
    private Map<String, Long> countByDocumentType;

    /** Répartition par statut : {"brouillon": 4, "valide": 20, "archive": 3, ...} */
    @Schema(description = "Compte par statut affiché, déduit des drapeaux du document : l'archive "
            + "prime sur l'obsolescence, celle-ci sur la validation. Les documents en cours de "
            + "circuit sont classés sous le code de leur étape, si bien que les clés ne sont pas un "
            + "ensemble fixe.",
            example = "{\"BROUILLON\": 4, \"VALIDE\": 20, \"VERIFICATION\": 3, \"ARCHIVE\": 1}")
    private Map<String, Long> countByStatus;

    /** Répartition par domaine : {"Qualité": 10, "RH": 6, ...} */
    @Schema(description = "Compte par libellé de domaine. Les documents antérieurs au référentiel "
            + "portent un libellé saisi en clair, qui peut dédoubler une même rubrique.",
            example = "{\"Qualité\": 10, \"RH\": 6}")
    private Map<String, Long> countByDomaine;

    /** Répartition par service (serviceLibelle) : {"DSI": 8, "DRH": 5, ...} */
    @Schema(description = "Compte par structure propriétaire, classé sur le libellé et non sur "
            + "l'identifiant : une structure renommée apparaît alors sous deux entrées.",
            example = "{\"DSI\": 8, \"DRH\": 5}")
    private Map<String, Long> countByService;

    /** Nombre de documents en retard de révision */
    @Schema(description = "Documents dont la revue périodique est échue. Le chiffre suit le "
            + "drapeau posé par la surveillance, non un calcul fait à la lecture.",
            example = "7")
    private long documentsEnRetardRevision;

    /** Nombre de documents confidentiels */
    @Schema(description = "Documents portant un niveau de confidentialité, quel qu'il soit.",
            example = "12")
    private long documentsConfidentiels;

    /** Nombre de documents externes */
    @Schema(description = "Documents reçus de l'extérieur, que l'organisation maîtrise sans les "
            + "rédiger.",
            example = "23")
    private long documentsExternes;
}
