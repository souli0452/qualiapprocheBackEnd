package com.qualiapproche.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.qualiapproche.dto.NonConformiteDto;
import com.qualiapproche.enumeration.Etat;

public interface NonConformiteService {
    NonConformiteDto createNonConformite(NonConformiteDto dto) throws IOException;

    NonConformiteDto updateNonConformite(UUID id, NonConformiteDto dto) throws IOException;
    List<NonConformiteDto> updateNonConformites(List<NonConformiteDto> dtos) throws IOException;
   // NonConformiteDto create(NonConformiteDto nonConformiteDto);
    NonConformiteDto update(NonConformiteDto nonConformiteDto);
    List<NonConformiteDto> allNonConformites();
    List<NonConformiteDto> findImupted(String userId,Etat etat);
    // Méthode pour récupérer les non-conformités par état
    List<NonConformiteDto> getNonConformitesByEtatNonConformite(Etat etat);
    List<NonConformiteDto> getNonConformitesByEtatAnStructure(Etat etat, String uuid);
    List<NonConformiteDto> getNonConformitesByEtatAndStructureOrigine(Etat etat, String uuid);

    NonConformiteDto getNonConformiteById(UUID id);

    void delete(UUID id);

}