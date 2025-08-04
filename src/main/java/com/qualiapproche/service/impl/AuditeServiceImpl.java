package com.qualiapproche.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qualiapproche.dto.AuditeDto;
import com.qualiapproche.entities.Audite;
import com.qualiapproche.entities.mappers.AuditeMapper;
import com.qualiapproche.repository.AuditeRepository;
import com.qualiapproche.service.AuditeService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class AuditeServiceImpl implements AuditeService {

    private final AuditeMapper auditeMapper;
    private final AuditeRepository auditeRepository;
    @Override
    public AuditeDto create(AuditeDto auditeDto) {
        Audite audite = auditeMapper.toEntity(auditeDto);
        return auditeMapper.toDto(auditeRepository.save(audite));
    }

    @Override
    public AuditeDto getAuditeById(UUID id) {
        if (auditeRepository.existsById(id)) {
            return auditeMapper.toDto(auditeRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce audite n'existe pas.");
        }
    }

    @Override
    public List<AuditeDto> allAudite() {
        return  auditeMapper.toDtos(auditeRepository.findAll()) ;
    }

    @Override
    public AuditeDto update(AuditeDto auditeDto) {
        return auditeRepository.findById(auditeDto.getId()).map(auditeExisted -> {
            auditeMapper.updateEntityFromDto(auditeDto, auditeExisted);
            return auditeMapper.toDto(auditeRepository.save(auditeExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun département trouvé."));
    }

    @Override
    public void delete(UUID id) {
        Audite audite=auditeRepository.getReferenceById(id);
        auditeRepository.delete(audite);
    }
}

