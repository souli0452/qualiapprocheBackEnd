package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.common.dto.NiveauNonConformiteDto;
import com.qualiapproche.amelioration.entities.NiveauNonConformite;
import com.qualiapproche.amelioration.entities.mappers.NiveauNonConformiteMapper;
import com.qualiapproche.amelioration.repository.NiveauNonConformiteRepository;
import com.qualiapproche.amelioration.service.NiveauNonConformiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NiveauNonConformiteServiceImpl implements NiveauNonConformiteService {

    private final NiveauNonConformiteMapper niveauNonConformiteMapper;
    private final NiveauNonConformiteRepository niveauNonConformiteRepository;


    @Override
    @Transactional
    public NiveauNonConformiteDto create(NiveauNonConformiteDto niveauNonConformiteDto) {
        NiveauNonConformite niveauNonConformite = niveauNonConformiteMapper.toEntity(niveauNonConformiteDto);
        niveauNonConformite = niveauNonConformiteRepository.save(niveauNonConformite);
        return niveauNonConformiteMapper.toDto((niveauNonConformite));
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
    public Page<NiveauNonConformiteDto> getAll(Pageable pageable) {
        return niveauNonConformiteRepository.findAll(pageable).map(niveauNonConformiteMapper::toDto);
    }

    @Override
    @Transactional
    public NiveauNonConformiteDto update(NiveauNonConformiteDto niveauNonConformiteDto) {
        return niveauNonConformiteRepository.findById(niveauNonConformiteDto.getId()).map(niveauNonConformiteExisted -> {
            niveauNonConformiteMapper.updateEntityFromDto(niveauNonConformiteDto, niveauNonConformiteExisted);
            niveauNonConformiteExisted = niveauNonConformiteRepository.save(niveauNonConformiteExisted);
            return niveauNonConformiteMapper.toDto((niveauNonConformiteExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun niveau non conformite trouvé."));
    }

    @Override
    public void delete(UUID id) {
        NiveauNonConformite niveauNonConformite = niveauNonConformiteRepository.getReferenceById(id);
        niveauNonConformiteRepository.delete(niveauNonConformite);
    }

    @Override
    public Page<NiveauNonConformiteDto> search(String libelle, String description, Pageable pageable) {
        return niveauNonConformiteRepository.findAll(pageable)
                .map(niveauNonConformiteMapper::toDto);
    }
}
