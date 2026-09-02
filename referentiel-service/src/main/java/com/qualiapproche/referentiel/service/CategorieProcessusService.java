package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.CategorieProcessusDto;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategorieProcessusService {
    CategorieProcessusDto create(CategorieProcessusDto categorieProcessusDto);
    CategorieProcessusDto update(CategorieProcessusDto categorieProcessusDto);
    CategorieProcessusDto getById(UUID id);
    Page<CategorieProcessusDto> getAll(Pageable pageable);
    void delete(UUID id);
}
