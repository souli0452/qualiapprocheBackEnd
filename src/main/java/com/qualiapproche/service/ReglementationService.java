package com.qualiapproche.service;

import com.qualiapproche.dto.ReglementationDto;

import java.util.List;
import java.util.UUID;

public interface ReglementationService {
    ReglementationDto create(ReglementationDto reglementationDto);
    ReglementationDto update(ReglementationDto reglementationDto);
    List<ReglementationDto> allReglementations();
    ReglementationDto getReglementationById(UUID id);

    void delete(UUID id);
}
