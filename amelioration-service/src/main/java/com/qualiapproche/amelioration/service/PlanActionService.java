package com.qualiapproche.amelioration.service;

import java.io.IOException;
import java.util.List;
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

    void updateWorkflowState(UUID planActionId, String newStateName, String newEtatTraitement);
}