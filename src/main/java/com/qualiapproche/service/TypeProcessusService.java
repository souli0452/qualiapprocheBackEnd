package com.qualiapproche.service;

import com.qualiapproche.dto.TypeProcessusDto;
import java.util.List;
import java.util.UUID;

public interface TypeProcessusService {
    TypeProcessusDto create(TypeProcessusDto typeProcessusDto);
    TypeProcessusDto update(TypeProcessusDto typeProcessusDto);
    TypeProcessusDto getById(UUID id);
    List<TypeProcessusDto> getAll();
    void delete(UUID id);
}
