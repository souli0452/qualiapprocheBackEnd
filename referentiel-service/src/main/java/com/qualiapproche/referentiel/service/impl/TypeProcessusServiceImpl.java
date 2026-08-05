package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.TypeProcessusDto;
import com.qualiapproche.referentiel.entities.TypeProcessus;
import com.qualiapproche.referentiel.entities.mappers.TypeProcessusMapper;
import com.qualiapproche.referentiel.repository.TypeProcessusRepository;
import com.qualiapproche.referentiel.service.TypeProcessusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TypeProcessusServiceImpl implements TypeProcessusService {

    private final TypeProcessusMapper typeProcessusMapper;
    private final TypeProcessusRepository typeProcessusRepository;


    @Override
    public TypeProcessusDto create(TypeProcessusDto typeProcessusDto) {
        TypeProcessus typeProcessus = typeProcessusMapper.toEntity(typeProcessusDto);
        return typeProcessusMapper.toDto(typeProcessusRepository.save(typeProcessus));
    }

    @Override
    public TypeProcessusDto getById(UUID id) {
        if (typeProcessusRepository.existsById(id)) {
            return typeProcessusMapper.toDto(typeProcessusRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce TypeProcessus n'existe pas.");
        }
    }

    @Override
    public Page<TypeProcessusDto> getAll(Pageable pageable) {
        return typeProcessusRepository.findAll(pageable).map(typeProcessusMapper::toDto);
    }

    @Override
    public TypeProcessusDto update(TypeProcessusDto typeProcessusDto) {
        return typeProcessusRepository.findById(typeProcessusDto.getId()).map(typeProcessusExisted -> {
            typeProcessusMapper.updateEntityFromDto(typeProcessusDto, typeProcessusExisted);
            return typeProcessusMapper.toDto(typeProcessusRepository.save(typeProcessusExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun type processus trouvé."));
    }

    @Override
    public void delete(UUID id) {
        TypeProcessus typeProcessus = typeProcessusRepository.getReferenceById(id);
        typeProcessusRepository.delete(typeProcessus);
    }
}
