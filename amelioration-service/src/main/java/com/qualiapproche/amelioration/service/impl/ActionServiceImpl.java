package com.qualiapproche.amelioration.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qualiapproche.common.dto.ActionDto;
import com.qualiapproche.amelioration.entities.Action;
import com.qualiapproche.amelioration.entities.mappers.ActionMapper;
import com.qualiapproche.amelioration.repository.ActionRepository;
import com.qualiapproche.amelioration.service.ActionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActionServiceImpl implements ActionService {

    private final ActionMapper actionMapper;
    private final ActionRepository actionRepository;


    @Override
    @org.springframework.transaction.annotation.Transactional
    public ActionDto create(ActionDto actionDto) {
        Action action = actionMapper.toEntity(actionDto);
        action = actionRepository.save(action);
        return actionMapper.toDto((action));
    }

    @Override
    public ActionDto getById(UUID id) {
        if (actionRepository.existsById(id)) {
            return actionMapper.toDto(actionRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce Action n'existe pas.");
        }
    }

    @Override
    public List<ActionDto> getAll() {
        return  actionMapper.toDtos(actionRepository.findAll()) ;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ActionDto update(ActionDto actionDto) {
        return actionRepository.findById(actionDto.getId()).map(ActionExisted -> {
            actionMapper.updateEntityFromDto(actionDto, ActionExisted);
            ActionExisted = actionRepository.save(ActionExisted);
            return actionMapper.toDto((ActionExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Aucun action trouvée."));
    }

    @Override
    public void delete(UUID id) {
        Action action=actionRepository.getReferenceById(id);
        actionRepository.delete(action);
    }
}
