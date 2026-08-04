package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.PrioriteDocumentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PrioriteDocumentService {
    PrioriteDocumentDto create(PrioriteDocumentDto dto);
    PrioriteDocumentDto update(PrioriteDocumentDto dto);
    PrioriteDocumentDto getById(UUID id);
    Page<PrioriteDocumentDto> getAll(Pageable pageable);
    List<PrioriteDocumentDto> getAll();
    void delete(UUID id);
}
