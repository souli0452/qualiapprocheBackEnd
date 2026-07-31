package com.qualiapproche.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Métadonnées modifiables d'un document.
 *
 * <p>Ne reprend délibérément ni le numéro de document, ni les compteurs de version, ni les
 * indicateurs pilotés par le circuit de validation ({@code esTraiter}, {@code obsolete},
 * {@code currentEtape}) : ceux-là relèvent du versionnage et du workflow, pas d'une saisie
 * libre. Un champ laissé à {@code null} n'est pas modifié.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdateDto {
    private String titre;
    private String reference;
    private String description;
    private String serviceId;
    private String serviceLibelle;
    private String serviceSigle;
    private String redacteur;
    private Integer periodiciteMois;
    private Boolean confidentiel;
    private Boolean documentExterne;
    private String processusDestId;
    private String processusDestLibelle;
    private String referenceOfficielle;
    private String domaine;
    private String statutLegal;

    /** Motif de la modification, journalisé dans la piste d'audit du document. */
    private String motif;
}
