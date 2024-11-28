package com.qualiapproche.service;

import com.qualiapproche.dto.DepartementDto;

import java.util.List;
import java.util.UUID;

public interface DepartementService {
    DepartementDto create(DepartementDto departementDto);

    DepartementDto getDepartementById(UUID id);

    List<DepartementDto> getallDepartement();

    DepartementDto update(DepartementDto departementDto);

    DepartementDto delete(UUID id);
}
