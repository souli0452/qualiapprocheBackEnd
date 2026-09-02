package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.CategorieProcessusDto;
import com.qualiapproche.referentiel.entities.CategorieProcessus;
import com.qualiapproche.referentiel.entities.mappers.CategorieProcessusMapper;
import com.qualiapproche.referentiel.repository.CategorieProcessusRepository;
import com.qualiapproche.referentiel.service.CategorieProcessusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategorieProcessusServiceImpl implements CategorieProcessusService {

    private final CategorieProcessusMapper categorieProcessusMapper;
    private final CategorieProcessusRepository categorieProcessusRepository;


    @Override
    public CategorieProcessusDto create(CategorieProcessusDto categorieProcessusDto) {
        CategorieProcessus categorieProcessus = categorieProcessusMapper.toEntity(categorieProcessusDto);
        return categorieProcessusMapper.toDto(categorieProcessusRepository.save(categorieProcessus));
    }

    @Override
    public CategorieProcessusDto getById(UUID id) {
        if (categorieProcessusRepository.existsById(id)) {
            return categorieProcessusMapper.toDto(categorieProcessusRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce CategorieProcessus n'existe pas.");
        }
    }

    @Override
    public Page<CategorieProcessusDto> getAll(Pageable pageable) {
        return categorieProcessusRepository.findAll(pageable).map(categorieProcessusMapper::toDto);
    }

    @Override
    public CategorieProcessusDto update(CategorieProcessusDto categorieProcessusDto) {
        return categorieProcessusRepository.findById(categorieProcessusDto.getId()).map(categorieProcessusExisted -> {
            categorieProcessusMapper.updateEntityFromDto(categorieProcessusDto, categorieProcessusExisted);
            return categorieProcessusMapper.toDto(categorieProcessusRepository.save(categorieProcessusExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun type processus trouvé."));
    }

    @Override
    public void delete(UUID id) {
        CategorieProcessus categorieProcessus = categorieProcessusRepository.getReferenceById(id);
        categorieProcessusRepository.delete(categorieProcessus);
    }
}
