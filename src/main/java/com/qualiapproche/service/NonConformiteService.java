package com.qualiapproche.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.qualiapproche.dto.FichierDto;
import com.qualiapproche.dto.NonConformiteDto;
import com.qualiapproche.enumeration.Etat;

public interface NonConformiteService {

    NonConformiteDto createNonConformite(NonConformiteDto dto) throws IOException;

    NonConformiteDto updateNonConformite(UUID id, NonConformiteDto dto) throws IOException;

    List<NonConformiteDto> updateNonConformites(List<NonConformiteDto> dtos) throws IOException;

    List<NonConformiteDto> getNonConformitesByEmail(String email);

    NonConformiteDto create(NonConformiteDto nonConformiteDto);

    NonConformiteDto update(NonConformiteDto nonConformiteDto);

    List<NonConformiteDto> allNonConformites();

    // Méthode pour récupérer les non-conformités par état
    List<NonConformiteDto> getNonConformitesByEtatNonConformite(Etat etat);

    NonConformiteDto getNonConformiteById(UUID id);

    void delete(UUID id);

}