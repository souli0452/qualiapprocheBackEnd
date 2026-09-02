package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.common.dto.SourceNonConformiteDto;
import com.qualiapproche.amelioration.entities.SourceDeNonConformite;
import com.qualiapproche.amelioration.entities.mappers.SourceNonConformiteMapper;
import com.qualiapproche.amelioration.repository.SourceNonConformiteRepository;
import com.qualiapproche.amelioration.service.SourceNonConformiteService;
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
public class SourceNonConformiteServiceImpl implements SourceNonConformiteService {

    private final SourceNonConformiteMapper sourceNonConformiteMapper;
    private final SourceNonConformiteRepository sourceNonConformiteRepository;

    @Override
    @Transactional
    public SourceNonConformiteDto create(SourceNonConformiteDto sourceNonConformiteDto) {
        SourceDeNonConformite sourceNonConformite = sourceNonConformiteMapper.toEntity(sourceNonConformiteDto);
        sourceNonConformite = sourceNonConformiteRepository.save(sourceNonConformite);
        return sourceNonConformiteMapper.toDto((sourceNonConformite));
    }

    @Override
    public SourceNonConformiteDto getById(UUID id) {
        if (sourceNonConformiteRepository.existsById(id)) {
            return sourceNonConformiteMapper.toDto(sourceNonConformiteRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce SourceNonConformite n'existe pas.");
        }
    }

    @Override
    public Page<SourceNonConformiteDto> getAll(Pageable pageable) {
        return sourceNonConformiteRepository.findAll(pageable).map(sourceNonConformiteMapper::toDto);
    }

    @Override
    @Transactional
    public SourceNonConformiteDto update(SourceNonConformiteDto sourceNonConformiteDto) {
        return sourceNonConformiteRepository.findById(sourceNonConformiteDto.getId()).map(sourceNonConformiteExisted -> {
            sourceNonConformiteMapper.updateEntityFromDto(sourceNonConformiteDto, sourceNonConformiteExisted);
            sourceNonConformiteExisted = sourceNonConformiteRepository.save(sourceNonConformiteExisted);
            return sourceNonConformiteMapper.toDto((sourceNonConformiteExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun type non conformité trouvé."));
    }

    @Override
    public void delete(UUID id) {
        SourceDeNonConformite sourceNonConformite = sourceNonConformiteRepository.getReferenceById(id);
        sourceNonConformiteRepository.delete(sourceNonConformite);
    }

    @Override
    public Page<SourceNonConformiteDto> search(String libelle, String description, Pageable pageable) {
        return sourceNonConformiteRepository.findAll(pageable)
                .map(sourceNonConformiteMapper::toDto);
    }
}

