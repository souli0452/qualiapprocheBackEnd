package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.DepartementDto;

import java.util.List;
import java.util.UUID;

public interface DepartementService {
    DepartementDto create(DepartementDto departementDto);

    DepartementDto getDepartementById(UUID id);

    List<DepartementDto> getallDepartement();

    DepartementDto update(DepartementDto departementDto);

    void delete(UUID id);
}
