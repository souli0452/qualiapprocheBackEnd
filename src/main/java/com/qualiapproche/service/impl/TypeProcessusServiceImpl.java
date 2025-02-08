package com.qualiapproche.service.impl;

import com.qualiapproche.dto.TypeProcessusDto;
import com.qualiapproche.entities.TypeProcessus;
import com.qualiapproche.entities.mappers.TypeProcessusMapper;
import com.qualiapproche.entities.mappers.TypeProcessusMapper;
import com.qualiapproche.repository.TypeProcessusRepository;
import com.qualiapproche.repository.TypeProcessusRepository;
import com.qualiapproche.service.TypeProcessusService;
import com.qualiapproche.service.TypeProcessusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TypeProcessusServiceImpl implements TypeProcessusService {

    private final TypeProcessusMapper typeProcessusMapper;
    private final TypeProcessusRepository typeProcessusRepository;


    @Override
    public TypeProcessusDto create(TypeProcessusDto typeProcessusDto) {
        TypeProcessus TypeProcessus = typeProcessusMapper.toEntity(typeProcessusDto);
        return typeProcessusMapper.toDto(typeProcessusRepository.save(TypeProcessus));
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
    public List<TypeProcessusDto> getAll() {
        return  typeProcessusMapper.toDtos(typeProcessusRepository.findAll()) ;
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
