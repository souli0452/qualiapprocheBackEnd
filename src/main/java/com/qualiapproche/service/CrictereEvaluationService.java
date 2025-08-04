package com.qualiapproche.service;

import com.qualiapproche.dto.CrictereEvaluationDto;

import java.util.List;
import java.util.UUID;

public interface CrictereEvaluationService {
    CrictereEvaluationDto create(CrictereEvaluationDto crictereEvaluationDto);

    CrictereEvaluationDto getCrictereEvaluationById(UUID id);

    List<CrictereEvaluationDto> allCrictereEvaluations();

    CrictereEvaluationDto update(CrictereEvaluationDto crictereEvaluationDto);

    void delete(UUID id);
}
