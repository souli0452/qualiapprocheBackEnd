package com.qualiapproche.amelioration.service.impl;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.amelioration.service.RisqueService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qualiapproche.common.dto.RisqueDto;
import com.qualiapproche.amelioration.entities.Risque;

import com.qualiapproche.amelioration.entities.mappers.RisqueMapper;

import com.qualiapproche.amelioration.repository.RisqueRepository;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class RisqueServiceImpl implements RisqueService {

    private final RisqueRepository risqueRepository;
    private final RisqueMapper risqueMapper;

    @Override
    @Transactional
    public RisqueDto create(RisqueDto risqueDto) {
        Risque risque = risqueMapper.toEntity(risqueDto);
        risque = risqueRepository.save(risque);
        return risqueMapper.toDto((risque));
    }

    @Override
    @Transactional
    public RisqueDto update(RisqueDto risqueDto) {
        return risqueRepository.findById(risqueDto.getId()).map(risqueExisted -> {
            risqueMapper.updateEntityFromDto(risqueDto, risqueExisted);
            risqueExisted = risqueRepository.save(risqueExisted);
            return risqueMapper.toDto((risqueExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucun risque trouvé."));
    }

    @Override
    public List<RisqueDto> allRisques() {
        return risqueMapper.toDtos(risqueRepository.findAll());
    }

    @Override
    public RisqueDto getRisqueById(UUID id) {
        if (risqueRepository.existsById(id)) {
            return risqueMapper.toDto(risqueRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce risque n'existe pas.");
        }
    }

    @Override
    public void delete(UUID id) {
        if (risqueRepository.existsById(id)) {
            risqueRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce risque n'existe pas.");
        }
    }
}
