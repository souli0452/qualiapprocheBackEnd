package com.qualiapproche.service.impl;

import com.qualiapproche.dto.FormationDto;
import com.qualiapproche.dto.FournisseurDto;
import com.qualiapproche.entities.Formation;
import com.qualiapproche.entities.Fournisseur;
import com.qualiapproche.entities.mappers.FormationMapper;
import com.qualiapproche.repository.FormationRepository;
import com.qualiapproche.service.FormationService;
import com.qualiapproche.utils.StatutEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormationServiceImpl implements FormationService {
    private final FormationRepository formationRepository;
    private final FormationMapper formationMapper;
    @Override
    public FormationDto create(FormationDto formationDto) {
        Formation formation=formationMapper.toEntity(formationDto);
        formation.setStatut(StatutEnum.ACTIF);
        return formationMapper.toDto(formationRepository.save(formation));
    }

    @Override
    public FormationDto update(FormationDto formationDto) {
        return formationRepository.findById(formationDto.getId()).map(formation -> {
            formationMapper.updateEntityFromDto(formationDto, formation);
            return formationMapper.toDto(formationRepository.save(formation));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune formation trouvée."));
    }

    @Override
    public FormationDto getById(UUID id) {
        return formationMapper.toDto(formationRepository.getReferenceById(id));
    }

    @Override
    public List<FormationDto> getAll() {
        return formationMapper.toDtos(formationRepository.findAll());
    }

    @Override
    public FormationDto delete(UUID id) {
        Formation formation=formationRepository.getReferenceById(id);
        formation.setStatut(StatutEnum.INACTIF);
        return formationMapper.toDto(formation);
    }
}
