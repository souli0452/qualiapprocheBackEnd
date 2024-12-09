package com.qualiapproche.service.impl;

import com.qualiapproche.dto.CrictereEvaluationDto;
import com.qualiapproche.entities.CrictereEvaluation;
import com.qualiapproche.entities.mappers.CritereEvaluationMapper;
import com.qualiapproche.repository.CrictereEvaluationRepository;
import com.qualiapproche.service.CrictereEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrictereEvaluationServiceImpl implements CrictereEvaluationService {

    private final CritereEvaluationMapper critereEvaluationMapper;
    private final CrictereEvaluationRepository crictereEvaluationRepository;

    @Override
    public CrictereEvaluationDto create(CrictereEvaluationDto crictereEvaluationDto) {
        CrictereEvaluation crictereEvaluation = critereEvaluationMapper.toEntity(crictereEvaluationDto);
        return critereEvaluationMapper.toDto(crictereEvaluationRepository.save(crictereEvaluation));
    }


    @Override
    public CrictereEvaluationDto getCrictereEvaluationById(UUID id) {
        if (crictereEvaluationRepository.existsById(id)) {
            return critereEvaluationMapper.toDto(crictereEvaluationRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce crictere d'évaluation n'existe pas.");
        }
    }

    @Override
    public List<CrictereEvaluationDto> allCrictereEvaluations() {
        return  critereEvaluationMapper.toDtos(crictereEvaluationRepository.findAll()) ;
    }

    @Override
    public CrictereEvaluationDto update(CrictereEvaluationDto crictereEvaluationDto) {
        return crictereEvaluationRepository.findById(crictereEvaluationDto.getId()).map(crictereEvaluationExisted -> {
            critereEvaluationMapper.updateEntityFromDto(crictereEvaluationDto, crictereEvaluationExisted);
            return critereEvaluationMapper.toDto(crictereEvaluationRepository.save(crictereEvaluationExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun crictère d'évaluation trouvé."));
    }

    @Override
    public void delete(UUID id) {
        CrictereEvaluation crictereEvaluation=crictereEvaluationRepository.getReferenceById(id);
        crictereEvaluationRepository.delete(crictereEvaluation);
    }
}
