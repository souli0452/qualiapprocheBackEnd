package com.qualiapproche.service.impl;

import com.qualiapproche.dto.EfficaciteDto;
import com.qualiapproche.entities.Efficacite;
import com.qualiapproche.entities.mappers.EfficaciteMapper;
import com.qualiapproche.entities.mappers.EfficaciteMapper;
import com.qualiapproche.repository.EfficaciteRepository;
import com.qualiapproche.repository.EfficaciteRepository;
import com.qualiapproche.service.EfficaciteService;
import com.qualiapproche.service.EfficaciteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EfficaciteServiceImpl implements EfficaciteService {

    private final EfficaciteMapper efficaciteMapper;
    private final EfficaciteRepository efficaciteRepository;


    @Override
    public EfficaciteDto create(EfficaciteDto efficaciteDto) {
        Efficacite efficacite = efficaciteMapper.toEntity(efficaciteDto);
        return efficaciteMapper.toDto(efficaciteRepository.save(efficacite));
    }

    @Override
    public EfficaciteDto getById(UUID id) {
        if (efficaciteRepository.existsById(id)) {
            return efficaciteMapper.toDto(efficaciteRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce Efficacite n'existe pas.");
        }
    }

    @Override
    public List<EfficaciteDto> getAll() {
        return  efficaciteMapper.toDtos(efficaciteRepository.findAll()) ;
    }

    @Override
    public EfficaciteDto update(EfficaciteDto efficaciteDto) {
        return efficaciteRepository.findById(efficaciteDto.getId()).map(efficaciteExisted -> {
            efficaciteMapper.updateEntityFromDto(efficaciteDto, efficaciteExisted);
            return efficaciteMapper.toDto(efficaciteRepository.save(efficaciteExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun département trouvé."));
    }

    @Override
    public void delete(UUID id) {
        Efficacite efficacite=efficaciteRepository.getReferenceById(id);
        efficaciteRepository.delete(efficacite);
    }
}
