package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.common.dto.EfficaciteDto;
import com.qualiapproche.amelioration.entities.Efficacite;
import com.qualiapproche.amelioration.entities.mappers.EfficaciteMapper;
import com.qualiapproche.amelioration.entities.mappers.EfficaciteMapper;
import com.qualiapproche.amelioration.repository.EfficaciteRepository;
import com.qualiapproche.amelioration.repository.EfficaciteRepository;
import com.qualiapproche.amelioration.service.EfficaciteService;
import com.qualiapproche.amelioration.service.EfficaciteService;
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
    @org.springframework.transaction.annotation.Transactional
    public EfficaciteDto create(EfficaciteDto efficaciteDto) {
        Efficacite efficacite = efficaciteMapper.toEntity(efficaciteDto);
        efficacite = efficaciteRepository.save(efficacite);
        return efficaciteMapper.toDto((efficacite));
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
    @org.springframework.transaction.annotation.Transactional
    public EfficaciteDto update(EfficaciteDto efficaciteDto) {
        return efficaciteRepository.findById(efficaciteDto.getId()).map(efficaciteExisted -> {
            efficaciteMapper.updateEntityFromDto(efficaciteDto, efficaciteExisted);
            efficaciteExisted = efficaciteRepository.save(efficaciteExisted);
            return efficaciteMapper.toDto((efficaciteExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun département trouvé."));
    }

    @Override
    public void delete(UUID id) {
        Efficacite efficacite=efficaciteRepository.getReferenceById(id);
        efficaciteRepository.delete(efficacite);
    }
}
