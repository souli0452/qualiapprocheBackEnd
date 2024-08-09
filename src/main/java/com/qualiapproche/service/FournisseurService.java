package com.qualiapproche.service;

import com.qualiapproche.dto.FournisseurDto;

import java.util.List;
import java.util.UUID;

public interface FournisseurService {
    FournisseurDto create(FournisseurDto fournisseurDto);
    FournisseurDto update(FournisseurDto fournisseurDto);
    List<FournisseurDto> allFournisseurs();
    FournisseurDto getFounisseurById(UUID id);
}
