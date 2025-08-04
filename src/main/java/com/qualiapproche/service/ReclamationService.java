package com.qualiapproche.service;

import com.qualiapproche.dto.ReclamationDto;

import java.util.List;
import java.util.UUID;

public interface ReclamationService {

    ReclamationDto create(ReclamationDto reclamationDto);
    ReclamationDto update(ReclamationDto reclamationDto);
    List<ReclamationDto> allReclamations();
    ReclamationDto getReclamationById(UUID id);

    void delete(UUID id);
}
