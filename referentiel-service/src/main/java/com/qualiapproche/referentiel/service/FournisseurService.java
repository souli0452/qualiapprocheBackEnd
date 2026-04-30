package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.CrictereEvaluationDto;
import com.qualiapproche.common.dto.FournisseurDto;
import com.qualiapproche.referentiel.entities.CrictereEvaluation;
import com.qualiapproche.referentiel.entities.Fournisseur;

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
