package com.qualiapproche.service;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.dto.RisqueDto;

public interface RisqueService {

    RisqueDto create(RisqueDto risqueDto);
    RisqueDto update(RisqueDto risqueDto);
    List<RisqueDto> allRisques();
    RisqueDto getRisqueById(UUID risqueId);

    void delete(UUID risqueId);

}
