package com.qualiapproche.service;

import com.qualiapproche.dto.EfficaciteDto;

import java.util.List;
import java.util.UUID;

public interface EfficaciteService {
    EfficaciteDto create(EfficaciteDto efficaciteDto);
    EfficaciteDto update(EfficaciteDto efficaciteDto);
    EfficaciteDto getById(UUID id);
    List<EfficaciteDto> getAll();

    void delete(UUID id);
}
