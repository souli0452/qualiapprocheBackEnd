package com.qualiapproche.amelioration.service;

import com.qualiapproche.common.dto.TypeNonConformiteDto;
import java.util.List;
import java.util.UUID;

public interface TypeNonConformiteService {
    TypeNonConformiteDto create(TypeNonConformiteDto typeNonConformiteDto);
    TypeNonConformiteDto update(TypeNonConformiteDto typeNonConformiteDto);
    TypeNonConformiteDto getById(UUID id);
    List<TypeNonConformiteDto> getAll();
    void delete(UUID id);
}
