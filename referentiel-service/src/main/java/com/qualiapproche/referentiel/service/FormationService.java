package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.FormationDto;


import java.util.List;
import java.util.UUID;

public interface FormationService {
    FormationDto create(FormationDto formationDto);
    FormationDto update(FormationDto formationDto);
    FormationDto getById(UUID id);
    List<FormationDto> getAll();

    FormationDto delete(UUID id);
}
