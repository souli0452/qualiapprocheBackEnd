package com.qualiapproche.service.impl;

import com.qualiapproche.dto.NiveauNonConformiteDto;
import com.qualiapproche.entities.NiveauNonConformite;
import com.qualiapproche.entities.mappers.NiveauNonConformiteMapper;
import com.qualiapproche.entities.mappers.NiveauNonConformiteMapper;
import com.qualiapproche.repository.NiveauNonConformiteRepository;
import com.qualiapproche.repository.NiveauNonConformiteRepository;
import com.qualiapproche.service.NiveauNonConformiteService;
import com.qualiapproche.service.NiveauNonConformiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NiveauNonConformiteServiceImpl implements NiveauNonConformiteService {

    private final NiveauNonConformiteMapper niveauNonConformiteMapper;
    private final NiveauNonConformiteRepository niveauNonConformiteRepository;


    @Override
    public NiveauNonConformiteDto create(NiveauNonConformiteDto niveauNonConformiteDto) {
        NiveauNonConformite niveauNonConformite = niveauNonConformiteMapper.toEntity(niveauNonConformiteDto);
        return niveauNonConformiteMapper.toDto(niveauNonConformiteRepository.save(niveauNonConformite));
    }

    @Override
    public NiveauNonConformiteDto getById(UUID id) {
        if (niveauNonConformiteRepository.existsById(id)) {
            return niveauNonConformiteMapper.toDto(niveauNonConformiteRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce NiveauNonConformite n'existe pas.");
        }
    }

    @Override
    public List<NiveauNonConformiteDto> getAll() {
        return  niveauNonConformiteMapper.toDtos(niveauNonConformiteRepository.findAll()) ;
    }

    @Override
    public NiveauNonConformiteDto update(NiveauNonConformiteDto niveauNonConformiteDto) {
        return niveauNonConformiteRepository.findById(niveauNonConformiteDto.getId()).map(niveauNonConformiteExisted -> {
            niveauNonConformiteMapper.updateEntityFromDto(niveauNonConformiteDto, niveauNonConformiteExisted);
            return niveauNonConformiteMapper.toDto(niveauNonConformiteRepository.save(niveauNonConformiteExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun niveau non conformite trouvé."));
    }

    @Override
    public void delete(UUID id) {
        NiveauNonConformite niveauNonConformite = niveauNonConformiteRepository.getReferenceById(id);
        niveauNonConformiteRepository.delete(niveauNonConformite);
    }
}
