package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.common.dto.TypeNonConformiteDto;
import com.qualiapproche.amelioration.entities.TypeNonConformite;
import com.qualiapproche.amelioration.entities.mappers.TypeNonConformiteMapper;
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.amelioration.service.TypeNonConformiteService;
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
public class TypeNonConformiteServiceImpl implements TypeNonConformiteService {

    private final TypeNonConformiteMapper typeNonConformiteMapper;
    private final TypeNonConformiteRepository typeNonConformiteRepository;

    @Override
    @Transactional
    public TypeNonConformiteDto create(TypeNonConformiteDto typeNonConformiteDto) {
        TypeNonConformite typeNonConformite = typeNonConformiteMapper.toEntity(typeNonConformiteDto);
        typeNonConformite = typeNonConformiteRepository.save(typeNonConformite);
        return typeNonConformiteMapper.toDto((typeNonConformite));
    }

    @Override
    public TypeNonConformiteDto getById(UUID id) {
        if (typeNonConformiteRepository.existsById(id)) {
            return typeNonConformiteMapper.toDto(typeNonConformiteRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce TypeNonConformite n'existe pas.");
        }
    }

    @Override
    public Page<TypeNonConformiteDto> getAll(Pageable pageable) {
        return typeNonConformiteRepository.findAll(pageable).map(typeNonConformiteMapper::toDto);
    }

    @Override
    @Transactional
    public TypeNonConformiteDto update(TypeNonConformiteDto typeNonConformiteDto) {
        return typeNonConformiteRepository.findById(typeNonConformiteDto.getId()).map(typeNonConformiteExisted -> {
            typeNonConformiteMapper.updateEntityFromDto(typeNonConformiteDto, typeNonConformiteExisted);
            typeNonConformiteExisted = typeNonConformiteRepository.save(typeNonConformiteExisted);
            return typeNonConformiteMapper.toDto((typeNonConformiteExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun type non conformité trouvé."));
    }

    @Override
    public void delete(UUID id) {
        TypeNonConformite typeNonConformite = typeNonConformiteRepository.getReferenceById(id);
        typeNonConformiteRepository.delete(typeNonConformite);
    }

    @Override
    public Page<TypeNonConformiteDto> search(String libelle, String description, Pageable pageable) {
        return typeNonConformiteRepository.findAll(pageable)
                .map(typeNonConformiteMapper::toDto);
    }
}

