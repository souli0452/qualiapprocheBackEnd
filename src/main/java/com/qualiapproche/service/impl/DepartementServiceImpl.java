package com.qualiapproche.service.impl;

import com.qualiapproche.dto.DepartementDto;
import com.qualiapproche.entities.Departement;
import com.qualiapproche.entities.mappers.DepartementMapper;
import com.qualiapproche.repository.DepartementRepository;
import com.qualiapproche.service.DepartementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartementServiceImpl implements DepartementService {

    private final DepartementMapper departementMapper;
    private final DepartementRepository departementRepository;
    @Override
    public DepartementDto create(DepartementDto departementDto) {
        Departement departement = departementMapper.toEntity(departementDto);
        return departementMapper.toDto(departementRepository.save(departement));
    }

    @Override
    public DepartementDto getDepartementById(UUID id) {
        if (departementRepository.existsById(id)) {
            return departementMapper.toDto(departementRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce departement n'existe pas.");
        }
    }

    @Override
    public List<DepartementDto> getallDepartement() {
        return  departementMapper.toDtos(departementRepository.findAll()) ;
    }

    @Override
    public DepartementDto update(DepartementDto departementDto) {
        return departementRepository.findById(departementDto.getId()).map(departementExisted -> {
            departementMapper.updateEntityFromDto(departementDto, departementExisted);
            return departementMapper.toDto(departementRepository.save(departementExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun département trouvé."));
    }

    @Override
    public DepartementDto delete(UUID id) {
        Departement departement=departementRepository.getReferenceById(id);
        return departementMapper.toDto(departement);
    }
}
