package com.qualiapproche.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Un écart relevé par un audit (M13), transmis au module Non-conformités (M05).
 *
 * <p>Ce qu'evaluation-service sait de l'écart, et rien de ce que M05 sait mieux que lui :
 * les libellés de l'origine et de la gravité, le sigle et la famille de processus de la structure
 * sont résolus à l'arrivée, depuis les référentiels de M05 et de referentiel-service.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NonConformiteDAuditDto {

    /**
     * L'écart d'origine : « AUDIT-CONSTAT:&lt;uuid&gt; » ou « AUDIT-RQAPBF:&lt;uuid&gt; ». Une
     * transmission rejouée avec la même clé rend le dossier déjà ouvert.
     */
    @NotBlank
    private String cleOrigine;

    /** La structure auditée : le dossier y est ouvert, et son pilote le reçoit. */
    @NotBlank
    private String structureConcerneeId;

    /** Le constat, rédigé pour être lu sans l'audit sous les yeux. */
    @NotBlank
    private String justification;

    /** D'où vient l'écart, lisible : « Audit AUD-2026-012 — constat 8.5 Enregistrements à jour ». */
    private String origineLibelle;

    /** L'origine dans le référentiel de M05 (« Audit interne »), quand le type d'audit la désigne. */
    private UUID typeNonConformiteId;

    /** La gravité dans le référentiel de M05, quand la nature du constat la désigne. */
    private UUID niveauNonConformiteId;

    /** Le processus visé, tel que l'audit le nomme. */
    private String nomProcessus;

    /** Soumettre au pilote dans la foulée ; sinon, le dossier reste en brouillon chez l'auditeur. */
    private boolean soumettre;
}
