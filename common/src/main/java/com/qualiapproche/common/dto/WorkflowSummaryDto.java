package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Vue minimale d'un workflow consommée par les services métier (amelioration-service,
 * support-service) via Feign. Remplace les anciens retours {@code Map<String, Object>}
 * qui exposaient des clés non typées ("id", "nom"...) sans garantie à la compilation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSummaryDto {
    private UUID id;
    private String nom;
    private String resourceType;
    private boolean actif;
    /**
     * Catégorie à laquelle le circuit est réservé au sein de sa famille — l'identifiant d'un type de
     * document — ou {@code null} s'il est le circuit par défaut de la famille.
     *
     * <p>Rendue aux modules métier pour qu'ils puissent constater ce que le moteur a retenu : le
     * rattrapage des réservations s'en sert pour être idempotent sans avoir à deviner.</p>
     */
    private String cibleId;
}
