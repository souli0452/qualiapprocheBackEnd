package com.qualiapproche.service.impl;

import com.qualiapproche.dto.ReglementationDto;
import com.qualiapproche.entities.Reglementation;
import com.qualiapproche.entities.mappers.ReglementationMapper;
import com.qualiapproche.repository.ReglementationRepository;
import com.qualiapproche.service.ReglementationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReglementationServiceImpl implements ReglementationService {

    private final ReglementationRepository reglementationRepository;
    private final ReglementationMapper reglementationMapper;

    @Override
    public ReglementationDto create(ReglementationDto reglementationDto) {
        Reglementation reglementation = reglementationMapper.toEntity(reglementationDto);
        return reglementationMapper.toDto(reglementationRepository.save(reglementation));
    }

    @Override
    public ReglementationDto update(ReglementationDto reglementationDto) {
        return reglementationRepository.findById(reglementationDto.getId()).map(reglementationExisted -> {
            reglementationMapper.updateEntityFromDto(reglementationDto, reglementationExisted);
            return reglementationMapper.toDto(reglementationRepository.save(reglementationExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune réglementation trouvée."));
    }

    @Override
    public List<ReglementationDto> allReglementations() {
        return  reglementationMapper.toDtos(reglementationRepository.findAll()) ;
    }

    @Override
    public ReglementationDto getReglementationById(UUID id) {
        if (reglementationRepository.existsById(id)) {
            return reglementationMapper.toDto(reglementationRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette reglementation n'existe pas.");
        }
    }

    @Override
    public void delete(UUID id) {
        if (reglementationRepository.existsById(id)) {
            reglementationRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette reglementation n'existe pas.");
        }
    }
}

