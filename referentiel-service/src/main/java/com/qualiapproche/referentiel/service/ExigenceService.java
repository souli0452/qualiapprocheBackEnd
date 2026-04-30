package com.qualiapproche.referentiel.service;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.common.dto.ExigenceDto;

public interface ExigenceService {
    ExigenceDto create(ExigenceDto departementDto);

    ExigenceDto getExigenceById(UUID id);

    List<ExigenceDto> allExigences();

    ExigenceDto update(ExigenceDto departementDto);

    void delete(UUID id);
}
