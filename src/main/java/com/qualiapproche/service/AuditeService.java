package com.qualiapproche.service;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.dto.AuditeDto;

public interface AuditeService {
    AuditeDto create(AuditeDto AuditeDto);

    AuditeDto update(AuditeDto AuditeDto);

    List<AuditeDto> allAudite();

    AuditeDto getAuditeById(UUID id);

    void delete(UUID id);
}
