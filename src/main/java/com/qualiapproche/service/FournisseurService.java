package com.qualiapproche.service;

import com.qualiapproche.dto.CrictereEvaluationDto;
import com.qualiapproche.dto.FournisseurDto;
import com.qualiapproche.entities.CrictereEvaluation;
import com.qualiapproche.entities.Fournisseur;

import java.util.List;
import java.util.UUID;

public interface FournisseurService {
    FournisseurDto create(FournisseurDto fournisseurDto);
    FournisseurDto update(FournisseurDto fournisseurDto);
    List<FournisseurDto> allFournisseurs();
    FournisseurDto getFounisseurById(UUID id);

    List<CrictereEvaluationDto> assignCriteresToFournisseur(UUID fournisseurId, List<UUID> critereEvaluationIds);

    Fournisseur getFournisseurWithCriteres(UUID fournisseurId);

    void delete(UUID id);
}
