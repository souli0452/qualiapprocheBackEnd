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
    /**
     * Page du référentiel, restreinte au terme cherché s'il y en a un.
     *
     * @param recherche terme libre, facultatif ; vide, la page porte tout le référentiel
     */
    Page<PrioriteDocumentDto> getAll(String recherche, Pageable pageable);
    List<PrioriteDocumentDto> getAll();
    void delete(UUID id);
}
