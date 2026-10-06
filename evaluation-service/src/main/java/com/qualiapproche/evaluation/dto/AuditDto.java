package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.evaluation.enumeration.NiveauRisque;
import com.qualiapproche.evaluation.enumeration.OrigineDocument;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Un audit tel que l'écran le lit et le saisit. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class AuditDto {
    private UUID id;
    private String reference;
    private String objectifsAudit;
    private String porteeAudit;
    private String criteresAudit;
    private LocalDate dateDebutPrevue;
    private LocalDate dateFinPrevue;
    private Integer dureeEstimeeJours;
    private NiveauRisque niveauRisque;
    private String descriptionRisque;
    private StatutAudit statut;
    /** Calculé : dates dépassées, quel que soit le statut (voir {@code Audit.estEnRetard}). */
    private boolean enRetard;
    /** Jours de retard sur la date qui compte : début si pas démarré, fin si en cours. Nul à l'heure. */
    private Long joursDeRetard;
    /** Score de maturité RQAP-BF de l'audit : sa dernière évaluation validée, à défaut la dernière. */
    private Double scoreMaturite;
    private OrigineDocument origineRapport;
    private boolean rapportCharge;
    private String referenceRapport;
    private String conclusionsRapport;
    private String recommandationsRapport;
    /** Pour un audit externe : l'organisme qui le mène. */
    private String organismeExterne;
    /** « ISO 9001 », « Grille RQAP-BF »... */
    private List<String> referentiels;
    /** Les domaines RQAP-BF concernés : des racines de l'arbre du référentiel. */
    private List<UUID> domaineRqapbfIds;
    private UUID typeAuditId;
    private String typeAuditLibelle;
    private boolean circuitExterne;
    /** Le processus audité : une structure de referentiel-service. */
    private UUID processusId;
    private List<UUID> siteIds;
    private UUID responsableEquipeId;
    private List<UUID> membreEquipeIds;
    private List<UUID> checklistIds;
    @Valid
    private List<ActionMaitriseRisqueDto> actionsMaitriseRisques;
}
