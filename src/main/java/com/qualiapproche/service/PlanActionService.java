package com.qualiapproche.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.qualiapproche.dto.PlanActionDto;

public interface PlanActionService {
    PlanActionDto createPlanActionDto(PlanActionDto dto) throws IOException;

    List<PlanActionDto> allPlanActions();

    PlanActionDto getPlanActionDtoById(UUID id);

    void delete(UUID id);

}