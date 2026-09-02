package com.qualiapproche.amelioration.service;

import com.qualiapproche.common.dto.SourceNonConformiteDto;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SourceNonConformiteService {
    SourceNonConformiteDto create(SourceNonConformiteDto sourceNonConformiteDto);
    SourceNonConformiteDto update(SourceNonConformiteDto sourceNonConformiteDto);
    SourceNonConformiteDto getById(UUID id);
    Page<SourceNonConformiteDto> getAll(Pageable pageable);
    void delete(UUID id);
    Page<SourceNonConformiteDto> search(String libelle, String description, Pageable pageable);
}
