package com.qualiapproche.referentiel.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qualiapproche.common.dto.ExigenceDto;
import com.qualiapproche.referentiel.entities.Exigence;
import com.qualiapproche.referentiel.entities.mappers.ExigenceMapper;
import com.qualiapproche.referentiel.repository.ExigenceRepository;
import com.qualiapproche.referentiel.service.ExigenceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExigenceServiceImpl implements ExigenceService {

    private final ExigenceRepository exigenceRepository;
    private final ExigenceMapper exigenceMapper;

    @Override
    public ExigenceDto create(ExigenceDto exigenceDto) {
        Exigence exigence = exigenceMapper.toEntity(exigenceDto);
        return exigenceMapper.toDto(exigenceRepository.save(exigence));
    }

    @Override
    public ExigenceDto update(ExigenceDto exigenceDto) {
        return exigenceRepository.findById(exigenceDto.getId()).map(exigenceExisted -> {
            exigenceMapper.updateEntityFromDto(exigenceDto, exigenceExisted);
            return exigenceMapper.toDto(exigenceRepository.save(exigenceExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune Exigence trouvée."));
    }

    @Override
    public List<ExigenceDto> allExigences() {
        return  exigenceMapper.toDtos(exigenceRepository.findAll()) ;
    }

    @Override
    public ExigenceDto getExigenceById(UUID id) {
        if (exigenceRepository.existsById(id)) {
            return exigenceMapper.toDto(exigenceRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette exigence n'existe pas.");
        }
    }

    @Override
    public void delete(UUID id) {
        if (exigenceRepository.existsById(id)) {
            exigenceRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette exigence n'existe pas.");
        }
    }
}