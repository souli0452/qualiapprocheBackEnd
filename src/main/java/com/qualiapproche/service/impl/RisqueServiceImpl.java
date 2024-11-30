package com.qualiapproche.service.impl;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.service.RisqueService;
import com.qualiapproche.utils.StatutEnum;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qualiapproche.dto.FormationDto;
import com.qualiapproche.dto.RisqueDto;
import com.qualiapproche.entities.Formation;
import com.qualiapproche.entities.Fournisseur;
import com.qualiapproche.entities.Risque;
import com.qualiapproche.entities.mappers.FounisseurMapper;
import com.qualiapproche.entities.mappers.RisqueMapper;
import com.qualiapproche.repository.FournisseurRepository;
import com.qualiapproche.repository.RisqueRepository;

@Service
@RequiredArgsConstructor
public class RisqueServiceImpl implements RisqueService {

    private final RisqueRepository risqueRepository;
    private final RisqueMapper risqueMapper;

    @Override
    public RisqueDto create(RisqueDto risqueDto) {
        Risque risque = risqueMapper.toEntity(risqueDto);
        return risqueMapper.toDto(risqueRepository.save(risque));
    }

    @Override
    public RisqueDto update(RisqueDto risqueDto) {
        return risqueRepository.findById(risqueDto.getId()).map(risqueExisted -> {
            risqueMapper.updateEntityFromDto(risqueDto, risqueExisted);
            return risqueMapper.toDto(risqueRepository.save(risqueExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucun risque trouvé."));
    }

    @Override
    public List<RisqueDto> allRisques() {
        return  risqueMapper.toDtos(risqueRepository.findAll()) ;
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
    public void delete(UUID risqueId) {
        if (risqueRepository.existsById(risqueId)) {
            risqueRepository.deleteById(risqueId);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce risque n'existe pas.");
        }
    }
}
