package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.ReglementationDto;

import java.util.List;
import java.util.UUID;

public interface ReglementationService {
    ReglementationDto create(ReglementationDto reglementationDto);
    ReglementationDto update(ReglementationDto reglementationDto);
    List<ReglementationDto> allReglementations();
    ReglementationDto getReglementationById(UUID id);

    void delete(UUID id);
}
