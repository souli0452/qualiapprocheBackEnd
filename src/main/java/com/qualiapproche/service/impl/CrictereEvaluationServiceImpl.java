package com.qualiapproche.service.impl;

import com.qualiapproche.dto.CrictereEvaluationDto;
import com.qualiapproche.entities.CrictereEvaluation;
import com.qualiapproche.entities.mappers.CritereEvaluationMapper;
import com.qualiapproche.repository.CrictereEvaluationRepository;
import com.qualiapproche.repository.FournisseurRepository;
import com.qualiapproche.service.CrictereEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
