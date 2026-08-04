package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.DomaineApplicationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DomaineApplicationService {
    DomaineApplicationDto create(DomaineApplicationDto dto);
    DomaineApplicationDto update(DomaineApplicationDto dto);
    DomaineApplicationDto getById(UUID id);
    Page<DomaineApplicationDto> getAll(Pageable pageable);
    List<DomaineApplicationDto> getAll();
    void delete(UUID id);
}
