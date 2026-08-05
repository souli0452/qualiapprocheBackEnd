package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.PrestataireDto;
import com.qualiapproche.referentiel.entities.Prestataire;
import com.qualiapproche.referentiel.entities.mappers.PrestataireMapper;
import com.qualiapproche.referentiel.repository.PrestataireRepository;
import com.qualiapproche.referentiel.service.PrestataireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrestataireServiceImpl implements PrestataireService {

    private final PrestataireRepository prestataireRepository;
    private final PrestataireMapper prestataireMapper;

    @Override
    public PrestataireDto create(PrestataireDto prestataireDto) {
        Prestataire prestataire = prestataireMapper.toEntity(prestataireDto);
        return prestataireMapper.toDto(prestataireRepository.save(prestataire));
    }

    @Override
    public PrestataireDto update(PrestataireDto prestataireDto) {
        return prestataireRepository.findById(prestataireDto.getId()).map(prestataireExisted -> {
            prestataireMapper.updateEntityFromDto(prestataireDto, prestataireExisted);
            return prestataireMapper.toDto(prestataireRepository.save(prestataireExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucun Prestataire trouvé."));
    }

    @Override
    public List<PrestataireDto> allPrestataires() {
        return prestataireMapper.toDtos(prestataireRepository.findAll());
    }

    @Override
    public PrestataireDto getPrestataireById(UUID id) {
        if (prestataireRepository.existsById(id)) {
            return prestataireMapper.toDto(prestataireRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce prestataire n'existe pas.");
        }
    }

    @Override
    public void delete(UUID id) {
        if (prestataireRepository.existsById(id)) {
            prestataireRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce prestataire n'existe pas.");
        }
    }
}

