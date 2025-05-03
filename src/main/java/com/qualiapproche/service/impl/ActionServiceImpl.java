package com.qualiapproche.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qualiapproche.dto.ActionDto;
import com.qualiapproche.entities.Action;
import com.qualiapproche.entities.mappers.ActionsMapper;
import com.qualiapproche.repository.ActionRepository;
import com.qualiapproche.service.ActionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActionServiceImpl implements ActionService {

    private final ActionsMapper actionsMapper;
    private final ActionRepository actionRepository;


    @Override
    public ActionDto create(ActionDto actionDto) {
        Action action = actionsMapper.toEntity(actionDto);
        return actionsMapper.toDto(actionRepository.save(action));
    }

    @Override
    public ActionDto getById(UUID id) {
        if (actionRepository.existsById(id)) {
            return actionsMapper.toDto(actionRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce Action n'existe pas.");
        }
    }

    @Override
    public List<ActionDto> getAll() {
        return  actionsMapper.toDtos(actionRepository.findAll()) ;
    }

    @Override
    public ActionDto update(ActionDto actionDto) {
        return actionRepository.findById(actionDto.getId()).map(ActionExisted -> {
            actionsMapper.updateEntityFromDto(actionDto, ActionExisted);
            return actionsMapper.toDto(actionRepository.save(ActionExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun action trouvée."));
    }

    @Override
    public void delete(UUID id) {
        Action action=actionRepository.getReferenceById(id);
        actionRepository.delete(action);
    }
}
