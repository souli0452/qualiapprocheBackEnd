package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.TypeProcessusDto;
import java.util.List;
import java.util.UUID;

public interface TypeProcessusService {
    TypeProcessusDto create(TypeProcessusDto typeProcessusDto);
    TypeProcessusDto update(TypeProcessusDto typeProcessusDto);
    TypeProcessusDto getById(UUID id);
    List<TypeProcessusDto> getAll();
    void delete(UUID id);
}
