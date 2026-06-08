package com.qualiapproche.amelioration.service;

import com.qualiapproche.common.dto.TypeNonConformiteDto;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TypeNonConformiteService {
    TypeNonConformiteDto create(TypeNonConformiteDto typeNonConformiteDto);
    TypeNonConformiteDto update(TypeNonConformiteDto typeNonConformiteDto);
    TypeNonConformiteDto getById(UUID id);
    Page<TypeNonConformiteDto> getAll(Pageable pageable);
    void delete(UUID id);
    Page<TypeNonConformiteDto> search(String libelle, String description, Pageable pageable);
}
