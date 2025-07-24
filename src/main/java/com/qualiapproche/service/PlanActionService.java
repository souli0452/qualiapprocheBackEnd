package com.qualiapproche.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.qualiapproche.dto.PlanActionDto;
import com.qualiapproche.utils.StatutEnum;

public interface PlanActionService {
    PlanActionDto createPlanActionDto(PlanActionDto dto) throws IOException;

    List<PlanActionDto> allPlanActions();
    List<PlanActionDto> planActionByResponsable(String responsable, StatutEnum statut);
    PlanActionDto getPlanActionDtoById(UUID id);
    List<PlanActionDto> planActionByResponsableAll(String responsable);
    PlanActionDto changeStatus(PlanActionDto dto);
    void delete(UUID id);

}