package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Ce qu'un module métier a besoin de savoir d'un circuit pour le désigner : "
        + "de quoi le choisir dans une liste et l'ouvrir sur un dossier. Ni ses étapes, ni ses "
        + "transitions, qui ne regardent que le moteur.")
public class WorkflowSummaryDto {
    @Schema(description = "Identifiant du circuit, à reprendre pour l'ouvrir sur une ressource ou "
            + "pour le désigner depuis un type de document.",
            example = "9b1f0c22-4b3e-4c2a-9a77-2f4d1b8e6c10")
    private UUID id;

    @Schema(description = "Nom donné au circuit par qui l'a configuré, destiné aux écrans "
            + "d'administration.",
            example = "Workflow Défaut Document")
    private String nom;

    @Schema(description = "Famille de dossiers à laquelle le circuit s'applique. C'est aussi "
            + "l'aiguillage de retour vers le module métier : hors de ces quatre valeurs, un "
            + "dossier n'aurait ni notification ni liste « à traiter ».",
            example = "DOCUMENT",
            allowableValues = {"DOCUMENT", "NON_CONFORMITE", "PLAN_ACTION", "DEMANDE_DOCUMENT"})
    private String resourceType;

    @Schema(description = "Le circuit est celui que les dossiers de sa famille empruntent "
            + "aujourd'hui. Un seul l'est à la fois par famille et par cible ; désactiver un "
            + "circuit ne touche pas aux dossiers qui le parcourent déjà.",
            example = "true")
    private boolean actif;
    /**
     * Catégorie à laquelle le circuit est réservé au sein de sa famille — l'identifiant d'un type de
     * document — ou {@code null} s'il est le circuit par défaut de la famille.
     *
     * <p>Rendue aux modules métier pour qu'ils puissent constater ce que le moteur a retenu : le
     * rattrapage des réservations s'en sert pour être idempotent sans avoir à deviner.</p>
     */
    @Schema(description = "Catégorie de dossiers à laquelle le circuit est réservé au sein de sa "
            + "famille — l'identifiant d'un type de document — ou vide s'il est le circuit par "
            + "défaut de la famille, celui que prennent les dossiers qu'aucune cible ne réclame. "
            + "Chaîne opaque : l'identifiant appartient à un autre service, et le moteur ne cherche "
            + "pas à savoir ce qu'il désigne.",
            example = "3f0b5d18-9c44-4f0b-8a71-6d2e5c7a9b31")
    private String cibleId;
}
