package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.NiveauConfidentialiteDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NiveauConfidentialiteService {
    NiveauConfidentialiteDto create(NiveauConfidentialiteDto dto);
    NiveauConfidentialiteDto update(NiveauConfidentialiteDto dto);
    NiveauConfidentialiteDto getById(UUID id);
    Page<NiveauConfidentialiteDto> getAll(Pageable pageable);
    List<NiveauConfidentialiteDto> getAll();
    void delete(UUID id);
}
