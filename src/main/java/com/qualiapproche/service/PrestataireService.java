package com.qualiapproche.service;

import com.qualiapproche.dto.PrestataireDto;

import java.util.List;
import java.util.UUID;

public interface PrestataireService {

    PrestataireDto create(PrestataireDto prestataireDto);
    PrestataireDto update(PrestataireDto prestataireDto);
    List<PrestataireDto> allPrestataires();
    PrestataireDto getPrestataireById(UUID id);

    void delete(UUID id);
}

