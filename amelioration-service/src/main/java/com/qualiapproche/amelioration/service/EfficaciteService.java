package com.qualiapproche.amelioration.service;

import com.qualiapproche.common.dto.EfficaciteDto;

import java.util.List;
import java.util.UUID;

public interface EfficaciteService {
    EfficaciteDto create(EfficaciteDto efficaciteDto);
    EfficaciteDto update(EfficaciteDto efficaciteDto);
    EfficaciteDto getById(UUID id);
    List<EfficaciteDto> getAll();

    void delete(UUID id);
}
