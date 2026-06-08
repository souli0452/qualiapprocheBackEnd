package com.qualiapproche.amelioration.service;

import com.qualiapproche.common.dto.NiveauNonConformiteDto;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NiveauNonConformiteService {
    NiveauNonConformiteDto create(NiveauNonConformiteDto niveauNonConformiteDto);
    NiveauNonConformiteDto update(NiveauNonConformiteDto niveauNonConformiteDto);
    NiveauNonConformiteDto getById(UUID id);
    Page<NiveauNonConformiteDto> getAll(Pageable pageable);
    void delete(UUID id);
    Page<NiveauNonConformiteDto> search(String libelle, String description, Pageable pageable);
}
