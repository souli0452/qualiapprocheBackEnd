package com.qualiapproche.amelioration.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.utils.StatutEnum;

public interface PlanActionService {
    PlanActionDto createPlanActionDto(PlanActionDto dto) throws IOException;

    List<PlanActionDto> allPlanActions();
    List<PlanActionDto> planActionByResponsable(String responsable, StatutEnum statut);
    PlanActionDto getPlanActionDtoById(UUID id);
    List<PlanActionDto> planActionByResponsableAll(String responsable);
    PlanActionDto changeStatus(PlanActionDto dto) throws IOException ;
    PlanActionDto rejet(PlanActionDto dto) throws IOException ;
    void delete(UUID id);
    Map<String, Map<String, Map<String, Long>>> getFrequenceTraitementParMois(int annee);

    List<PlanActionDto> getPlanActionsByStructure(String structureId);
}