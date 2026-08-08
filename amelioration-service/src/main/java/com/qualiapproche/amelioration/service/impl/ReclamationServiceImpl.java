package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.common.dto.ReclamationDto;
import com.qualiapproche.amelioration.entities.Reclamation;
import com.qualiapproche.amelioration.entities.mappers.ReclamationMapper;
import com.qualiapproche.amelioration.repository.ReclamationRepository;
import com.qualiapproche.amelioration.service.ReclamationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final ReclamationMapper reclamationMapper;

    @Override
    @Transactional
    public ReclamationDto create(ReclamationDto reclamationDto) {
        Reclamation reclamation = reclamationMapper.toEntity(reclamationDto);
        reclamation = reclamationRepository.save(reclamation);
        return reclamationMapper.toDto((reclamation));
    }

    @Override
    @Transactional
    public ReclamationDto update(ReclamationDto reclamationDto) {
        return reclamationRepository.findById(reclamationDto.getId()).map(reclamationExisted -> {
            reclamationMapper.updateEntityFromDto(reclamationDto, reclamationExisted);
            reclamationExisted = reclamationRepository.save(reclamationExisted);
            return reclamationMapper.toDto((reclamationExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune reclamation trouvée."));
    }

    @Override
    public List<ReclamationDto> allReclamations() {
        return reclamationMapper.toDtos(reclamationRepository.findAll());
    }

    @Override
    public ReclamationDto getReclamationById(UUID id) {
        if (reclamationRepository.existsById(id)) {
            return reclamationMapper.toDto(reclamationRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette reclamation n'existe pas.");
        }
    }

    @Override
    public void delete(UUID id) {
        if (reclamationRepository.existsById(id)) {
            reclamationRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette reclamation n'existe pas.");
        }
    }
}
