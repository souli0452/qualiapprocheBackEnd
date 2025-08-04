package com.qualiapproche.service.impl;

import com.qualiapproche.dto.ReclamationDto;
import com.qualiapproche.entities.Reclamation;
import com.qualiapproche.entities.mappers.ReclamationMapper;
import com.qualiapproche.repository.ReclamationRepository;
import com.qualiapproche.service.ReclamationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final ReclamationMapper reclamationMapper;

    @Override
    public ReclamationDto create(ReclamationDto reclamationDto) {
        Reclamation reclamation = reclamationMapper.toEntity(reclamationDto);
        return reclamationMapper.toDto(reclamationRepository.save(reclamation));
    }

    @Override
    public ReclamationDto update(ReclamationDto reclamationDto) {
        return reclamationRepository.findById(reclamationDto.getId()).map(reclamationExisted -> {
            reclamationMapper.updateEntityFromDto(reclamationDto, reclamationExisted);
            return reclamationMapper.toDto(reclamationRepository.save(reclamationExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune reclamation trouvée."));
    }

    @Override
    public List<ReclamationDto> allReclamations() {
        return  reclamationMapper.toDtos(reclamationRepository.findAll()) ;
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
