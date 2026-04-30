package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.CategorieFichierDto;

import java.util.List;
import java.util.UUID;

public interface CategorieFichierService {
    CategorieFichierDto create(CategorieFichierDto categorieFichierDto);

    CategorieFichierDto update(CategorieFichierDto categorieFichierDto);

    List<CategorieFichierDto> allCategorieFichier();

    CategorieFichierDto getCategorieFichierById(UUID id);
}
