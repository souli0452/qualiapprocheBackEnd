package com.qualiapproche.amelioration.service;

import com.qualiapproche.common.dto.NiveauNonConformiteDto;

import java.util.List;
import java.util.UUID;

public interface NiveauNonConformiteService {
    NiveauNonConformiteDto create(NiveauNonConformiteDto niveauNonConformiteDto);
    NiveauNonConformiteDto update(NiveauNonConformiteDto niveauNonConformiteDto);
    NiveauNonConformiteDto getById(UUID id);
    List<NiveauNonConformiteDto> getAll();
    void delete(UUID id);
}
