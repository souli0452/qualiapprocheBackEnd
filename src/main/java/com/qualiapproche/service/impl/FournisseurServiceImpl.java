package com.qualiapproche.service.impl;

import com.qualiapproche.dto.CrictereEvaluationDto;
import com.qualiapproche.dto.FournisseurDto;
import com.qualiapproche.entities.CrictereEvaluation;
import com.qualiapproche.entities.Fournisseur;
import com.qualiapproche.entities.mappers.CritereEvaluationMapper;
import com.qualiapproche.entities.mappers.FounisseurMapper;
import com.qualiapproche.repository.CrictereEvaluationRepository;
import com.qualiapproche.repository.FournisseurRepository;
import com.qualiapproche.service.FournisseurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class FournisseurServiceImpl implements FournisseurService {
    private final FournisseurRepository fournisseurRepository;
    private final FounisseurMapper founisseurMapper;
    private final CritereEvaluationMapper critereEvaluationMapper;
    private final CrictereEvaluationRepository crictereEvaluationRepository;

    @Override
    public FournisseurDto create(FournisseurDto fournisseurDto) {
        Fournisseur fournisseur = founisseurMapper.toEntity(fournisseurDto);
        return founisseurMapper.toDto(fournisseurRepository.save(fournisseur));
    }

    @Override
    public FournisseurDto update(FournisseurDto fournisseurDto) {
        return fournisseurRepository.findById(fournisseurDto.getId()).map(fournisseurExisted -> {
            founisseurMapper.updateEntityFromDto(fournisseurDto, fournisseurExisted);
            return founisseurMapper.toDto(fournisseurRepository.save(fournisseurExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucun fournisseur trouvé."));
    }

    @Override
    public List<FournisseurDto> allFournisseurs() {
        return  founisseurMapper.toDtos(fournisseurRepository.findAll()) ;
    }

    @Override
    public FournisseurDto getFounisseurById(UUID id) {
        if (fournisseurRepository.existsById(id)) {
            return founisseurMapper.toDto(fournisseurRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce fournisseur n'existe pas.");

        }
    }

    @Override
    public List<CrictereEvaluationDto> assignCriteresToFournisseur(UUID fournisseurId, List<UUID> critereEvaluationIds) {
        Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                .orElseThrow(() -> new RuntimeException("Fournisseur n'existe pas."));
        List<CrictereEvaluation> criteresEvaluation = critereEvaluationIds.stream()
                .map(id -> crictereEvaluationRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Critère d'évaluation n'existe pas.")))
                .peek(critere -> critere.setFournisseur(fournisseur))
                .collect(Collectors.toList());
        List<CrictereEvaluation> savedCriteres = crictereEvaluationRepository.saveAll(criteresEvaluation);
        return savedCriteres.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private CrictereEvaluationDto convertToDto(CrictereEvaluation critereEvaluation) {
        return new CrictereEvaluationDto(
                critereEvaluation.getId(),
                critereEvaluation.getLibelleCrictereEvaluation(),
                critereEvaluation.getDescriptionCrictereEvaluation(),
                critereEvaluation.getNoteAtribuerCritere(),
                critereEvaluation.getServiceClient(),
                critereEvaluation.getCommentaireEvaluation(),
                critereEvaluation.getDelaisLivraison(),
                critereEvaluation.getFournisseur().getId() // Retourne uniquement l'ID du fournisseur
        );
    }




    @Override
    public Fournisseur getFournisseurWithCriteres(UUID fournisseurId) {
        return fournisseurRepository.findByIdWithCriteres(fournisseurId);
             //   .orElseThrow(() -> new RuntimeException("Fournisseur n'existe pas."));
    }
}
