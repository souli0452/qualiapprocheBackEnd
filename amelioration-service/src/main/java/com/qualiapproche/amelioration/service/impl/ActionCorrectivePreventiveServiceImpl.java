package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.common.dto.ActionCorrectivePreventiveDto;
import com.qualiapproche.amelioration.entities.ActionCorrectivePreventive;
import com.qualiapproche.amelioration.entities.ActionExisgence;
import com.qualiapproche.amelioration.entities.ActionRisque;
import com.qualiapproche.amelioration.entities.Risque;
import com.qualiapproche.amelioration.entities.mappers.ActionCorrectivePreventiveMapper;
import com.qualiapproche.amelioration.repository.ActionCorrectivePreventiveRepository;
import com.qualiapproche.amelioration.repository.ActionExigenceRepository;
import com.qualiapproche.amelioration.repository.ActionRisqueRepossitory;
import com.qualiapproche.amelioration.service.ActionCorrectivePreventiveService;
import com.qualiapproche.common.utils.StatutEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActionCorrectivePreventiveServiceImpl implements ActionCorrectivePreventiveService {
    private final ActionCorrectivePreventiveRepository repository;
    private final ActionCorrectivePreventiveMapper actionMapper;
    private final ActionRisqueRepossitory actionRisqueRepossitory;
    private final ActionExigenceRepository actionExigenceRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ActionCorrectivePreventiveDto createAction(ActionCorrectivePreventiveDto actionCorrectivePreventiveDto) {
        ActionCorrectivePreventive actionCorrectivePreventive = actionMapper.toEntity(actionCorrectivePreventiveDto);
        actionCorrectivePreventive.setStatut(StatutEnum.ACTIF);
        
        // On sauvegarde l'action d'abord pour avoir son ID
        ActionCorrectivePreventive savedAction = repository.save(actionCorrectivePreventive);
        
        if (actionCorrectivePreventiveDto.getRisques() != null && !actionCorrectivePreventiveDto.getRisques().isEmpty()) {
            actionCorrectivePreventiveDto.getRisques().forEach(risque -> {
                ActionRisque actionRisque = new ActionRisque();
                actionRisque.setAction(savedAction);
                Risque risqueEntity = new Risque();
                risqueEntity.setId(risque.getId());
                actionRisque.setRisque(risqueEntity);
                actionRisqueRepossitory.save(actionRisque);
            });
        }
        if (actionCorrectivePreventiveDto.getExigences() != null && !actionCorrectivePreventiveDto.getExigences().isEmpty()) {
            actionCorrectivePreventiveDto.getExigences().forEach(exigence -> {
                ActionExisgence actionexigence = new ActionExisgence();
                actionexigence.setAction(savedAction);
                actionexigence.setExigenceId(exigence.getId());
                actionExigenceRepository.save(actionexigence);
            });
        }
        return actionMapper.toDto(savedAction);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ActionCorrectivePreventiveDto updateAction(ActionCorrectivePreventiveDto actionCorrectivePreventiveDto) {
        ActionCorrectivePreventive actionCorrectivePreventive = repository.getReferenceById(actionCorrectivePreventiveDto.getId());
        if (Objects.nonNull(actionCorrectivePreventive)) {
            actionMapper.updateEntityFromDto(actionCorrectivePreventiveDto, actionCorrectivePreventive);
            actionCorrectivePreventive = repository.save(actionCorrectivePreventive);
            if (actionCorrectivePreventiveDto.getRisques() != null && !actionCorrectivePreventiveDto.getRisques().isEmpty()) {
                deleteActionRisque(actionCorrectivePreventive.getId());
                ActionCorrectivePreventive finalActionCorrectivePreventive = actionCorrectivePreventive;
                actionCorrectivePreventiveDto.getRisques().forEach(risque -> {
                    ActionRisque actionRisque = new ActionRisque();
                    actionRisque.setAction(finalActionCorrectivePreventive);
                    Risque risqueEntity = new Risque();
                    risqueEntity.setId(risque.getId());
                    actionRisque.setRisque(risqueEntity);
                    actionRisqueRepossitory.save(actionRisque);
                });
            }
            if (actionCorrectivePreventiveDto.getExigences() != null && !actionCorrectivePreventiveDto.getExigences().isEmpty()) {
                deleteActionExigence(actionCorrectivePreventive.getId());
                ActionCorrectivePreventive finalActionCorrectivePreventive1 = actionCorrectivePreventive;
                actionCorrectivePreventiveDto.getExigences().forEach(exigence -> {
                    ActionExisgence actionexigence = new ActionExisgence();
                    actionexigence.setAction(finalActionCorrectivePreventive1);
                    actionexigence.setExigenceId(exigence.getId());
                    actionExigenceRepository.save(actionexigence);
                });
            }
        }
        return actionMapper.toDto(actionCorrectivePreventive);
    }

    @Override
    public ActionCorrectivePreventiveDto getActionById(UUID id) {
        return actionMapper.toDto(repository.getReferenceById(id));
    }

    @Override
    public List<ActionCorrectivePreventiveDto> getAlls() {
        return actionMapper.toDtos(repository.findAll());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ActionCorrectivePreventiveDto deleteAction(UUID id) {
        ActionCorrectivePreventive actionCorrectivePreventive = repository.getReferenceById(id);
        actionCorrectivePreventive.setStatut(StatutEnum.INACTIF);
        actionCorrectivePreventive = repository.save(actionCorrectivePreventive);
        return actionMapper.toDto(actionCorrectivePreventive);
    }

    public void deleteActionExigence(UUID id) {
        List<ActionExisgence> exisgences = actionExigenceRepository.findActionExisgenceByAction_Id(id);
        exisgences.forEach(actionExisgence -> {
            actionExigenceRepository.deleteById(actionExisgence.getId());
        });

    }

    public void deleteActionRisque(UUID id) {
        List<ActionRisque> actionRisques = actionRisqueRepossitory.findActionRisqueByAction_Id(id);
        actionRisques.forEach(actionExisgence -> {
            actionExigenceRepository.deleteById(actionExisgence.getId());
        });
    }
}
