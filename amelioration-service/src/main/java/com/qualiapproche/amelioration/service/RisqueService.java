package com.qualiapproche.amelioration.service;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.common.dto.RisqueDto;

public interface RisqueService {

    RisqueDto create(RisqueDto risqueDto);
    RisqueDto update(RisqueDto risqueDto);
    List<RisqueDto> allRisques();
    RisqueDto getRisqueById(UUID risqueId);

    void delete(UUID risqueId);

}
