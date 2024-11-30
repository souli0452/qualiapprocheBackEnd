package com.qualiapproche.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.qualiapproche.service.NonConformiteService;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import com.qualiapproche.dto.NonConformiteDto;
import com.qualiapproche.entities.NonConformite;
import com.qualiapproche.entities.mappers.NonConformiteMapper;
import com.qualiapproche.repository.NonConformiteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NonConformiteServiceImpl implements NonConformiteService {

    private final NonConformiteRepository nonConformiteRepository;
    private final NonConformiteMapper nonConformiteMapper;

    @Override
    public NonConformiteDto create(NonConformiteDto nonConformiteDto) {
        NonConformite nonConformite = nonConformiteMapper.toEntity(nonConformiteDto);
        return nonConformiteMapper.toDto(nonConformiteRepository.save(nonConformite));
    }

    @Override
    public NonConformiteDto update(NonConformiteDto nonConformiteDto) {
        return nonConformiteRepository.findById(nonConformiteDto.getId()).map(nonConformiteExisted -> {
            nonConformiteMapper.updateEntityFromDto(nonConformiteDto, nonConformiteExisted);
            return nonConformiteMapper.toDto(nonConformiteRepository.save(nonConformiteExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune NonConformité trouvée."));
    }

    @Override
    public List<NonConformiteDto> allNonConformites() {
        return  nonConformiteMapper.toDtos(nonConformiteRepository.findAll()) ;
    }

    @Override
    public NonConformiteDto getNonConformiteById(UUID id) {
        if (nonConformiteRepository.existsById(id)) {
            return nonConformiteMapper.toDto(nonConformiteRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette NonConformité n'existe pas.");
        }
    }

    @Override
    public void delete(UUID id) {
        if (nonConformiteRepository.existsById(id)) {
            nonConformiteRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette NonConformité n'existe pas.");
        }
    }
}
