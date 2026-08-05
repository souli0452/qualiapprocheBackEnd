package com.qualiapproche.amelioration.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.utils.StatutEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlanActionService {
    PlanActionDto createPlanActionDto(PlanActionDto dto) throws IOException;

    Page<PlanActionDto> allPlanActions(Pageable pageable);
    Page<PlanActionDto> planActionByResponsable(String responsable, StatutEnum statut, Pageable pageable);
    PlanActionDto getPlanActionDtoById(UUID id);
    Page<PlanActionDto> planActionByResponsableAll(String responsable, Pageable pageable);
    void delete(UUID id);
    Map<String, Map<String, Map<String, Long>>> getFrequenceTraitementParMois(int annee);

    Page<PlanActionDto> getPlanActionsByStructure(String structureId, Pageable pageable);

    Page<PlanActionDto> search(
            String numeroOdre, String responsableEmail, String responsableNomComplet,
            String numeroNc, StatutEnum status, UUID nonConformeId,
            java.time.LocalDate dateEcheanceFrom, java.time.LocalDate dateEcheanceTo,
            java.time.LocalDate dateTraitementFrom, java.time.LocalDate dateTraitementTo,
            Pageable pageable);

    /**
     * Corrige la description d'un plan d'action, sans toucher à son avancement.
     *
     * <p>L'agent imputé rédige ses plans avant de soumettre son traitement, et doit pouvoir les
     * reprendre : une cause mal formulée, une échéance à ajuster, un responsable à changer. Le
     * point d'entrée que le front appelait pour cela — {@code PUT /plan-action/update} — n'existait
     * pas : la correction partait en 404 et l'écran annonçait pourtant un succès.</p>
     *
     * <p>Le statut, le circuit et le rattachement à la non-conformité ne sont pas modifiables ici :
     * ils relèvent du moteur et de la décision, non de la saisie. Une correction ne doit ni engager
     * un plan, ni le solder.</p>
     */
    PlanActionDto corriger(PlanActionDto dto);

    /**
     * Actions correctives sur lesquelles l'appelant a une décision à prendre.
     *
     * <p>C'est le circuit qui sait qui peut agir : une action revenue chez le pilote pour être
     * vérifiée, ou déclinée et à ré-attribuer, n'apparaissait dans aucune de ses listes — il fallait
     * qu'il ouvre la non-conformité et en parcoure les actions pour la retrouver.</p>
     */
    org.springframework.data.domain.Page<PlanActionDto> aTraiterParLAppelant(
            org.springframework.data.domain.Pageable pageable);

    /**
     * Répercute sur le plan l'étape que le circuit vient d'atteindre, et les valeurs qui y ont été
     * saisies — dont le responsable, qu'une ré-attribution peut changer.
     */
    void updateWorkflowState(UUID planActionId, String newStateName, String newEtatTraitement,
            java.util.Map<String, String> champs);
}
