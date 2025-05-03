package com.qualiapproche.service.impl;

import com.qualiapproche.dto.TypeNonConformiteDto;
import com.qualiapproche.entities.TypeNonConformite;
import com.qualiapproche.entities.mappers.TypeNonConformiteMapper;
import com.qualiapproche.entities.mappers.TypeNonConformiteMapper;
import com.qualiapproche.repository.TypeNonConformiteRepository;
import com.qualiapproche.repository.TypeNonConformiteRepository;
import com.qualiapproche.service.TypeNonConformiteService;
import com.qualiapproche.service.TypeNonConformiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TypeNonConformiteServiceImpl implements TypeNonConformiteService {

    private final TypeNonConformiteMapper typeNonConformiteMapper;
    private final TypeNonConformiteRepository typeNonConformiteRepository;


    @Override
    public TypeNonConformiteDto create(TypeNonConformiteDto typeNonConformiteDto) {
        TypeNonConformite typeNonConformite = typeNonConformiteMapper.toEntity(typeNonConformiteDto);
        System.out.println(typeNonConformite);
        return typeNonConformiteMapper.toDto(typeNonConformiteRepository.save(typeNonConformite));
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
    public List<TypeNonConformiteDto> getAll() {
        return  typeNonConformiteMapper.toDtos(typeNonConformiteRepository.findAll()) ;
    }

    @Override
    public TypeNonConformiteDto update(TypeNonConformiteDto typeNonConformiteDto) {
        return typeNonConformiteRepository.findById(typeNonConformiteDto.getId()).map(typeNonConformiteExisted -> {
            typeNonConformiteMapper.updateEntityFromDto(typeNonConformiteDto, typeNonConformiteExisted);
            return typeNonConformiteMapper.toDto(typeNonConformiteRepository.save(typeNonConformiteExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun type non conformité trouvé."));
    }

    @Override
    public void delete(UUID id) {
        TypeNonConformite typeNonConformite=typeNonConformiteRepository.getReferenceById(id);
        typeNonConformiteRepository.delete(typeNonConformite);
    }
}
