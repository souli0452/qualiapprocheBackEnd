package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.CrictereEvaluationDto;
import com.qualiapproche.common.dto.FournisseurDto;
import com.qualiapproche.referentiel.entities.CrictereEvaluation;
import com.qualiapproche.referentiel.entities.Fournisseur;
import com.qualiapproche.referentiel.entities.mappers.CritereEvaluationMapper;
import com.qualiapproche.referentiel.entities.mappers.FounisseurMapper;
import com.qualiapproche.referentiel.repository.CrictereEvaluationRepository;
import com.qualiapproche.referentiel.repository.FournisseurRepository;
import com.qualiapproche.referentiel.service.FournisseurService;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fournisseur n'existe pas."));
        
        List<CrictereEvaluation> criteresEvaluation = critereEvaluationIds.stream()
                .map(id -> crictereEvaluationRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Critère d'évaluation n'existe pas.")))
                .peek(critere -> critere.setFournisseur(fournisseur))
                .collect(Collectors.toList());
        
        List<CrictereEvaluation> savedCriteres = crictereEvaluationRepository.saveAll(criteresEvaluation);
        return critereEvaluationMapper.toDtos(savedCriteres);
    }


    @Override
    public Fournisseur getFournisseurWithCriteres(UUID fournisseurId) {
        Fournisseur fournisseur = fournisseurRepository.findByIdWithCriteres(fournisseurId);
        if (fournisseur == null) {
            throw new RuntimeException("Fournisseur n'existe pas.");
        }
        return fournisseur;
    }


    @Override
    public void delete(UUID id) {
        Fournisseur fournisseur=fournisseurRepository.getReferenceById(id);
        fournisseurRepository.delete(fournisseur);
    }
}
