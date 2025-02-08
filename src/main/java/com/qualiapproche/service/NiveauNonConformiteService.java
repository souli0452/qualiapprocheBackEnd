package com.qualiapproche.service;

import com.qualiapproche.dto.NiveauNonConformiteDto;

import java.util.List;
import java.util.UUID;

public interface NiveauNonConformiteService {
    NiveauNonConformiteDto create(NiveauNonConformiteDto niveauNonConformiteDto);
    NiveauNonConformiteDto update(NiveauNonConformiteDto niveauNonConformiteDto);
    NiveauNonConformiteDto getById(UUID id);
    List<NiveauNonConformiteDto> getAll();
    void delete(UUID id);
}
