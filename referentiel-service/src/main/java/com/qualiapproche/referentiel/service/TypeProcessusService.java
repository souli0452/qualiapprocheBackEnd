package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.TypeProcessusDto;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TypeProcessusService {
    TypeProcessusDto create(TypeProcessusDto typeProcessusDto);
    TypeProcessusDto update(TypeProcessusDto typeProcessusDto);
    TypeProcessusDto getById(UUID id);
    Page<TypeProcessusDto> getAll(Pageable pageable);
    void delete(UUID id);
}
