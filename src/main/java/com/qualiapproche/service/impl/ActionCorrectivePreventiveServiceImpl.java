package com.qualiapproche.service.impl;

import com.qualiapproche.dto.ActionCorrectivePreventiveDto;
import com.qualiapproche.entities.ActionCorrectivePreventive;
import com.qualiapproche.entities.ActionExisgence;
import com.qualiapproche.entities.ActionRisque;
import com.qualiapproche.entities.Exigence;
import com.qualiapproche.entities.mappers.ActionMapper;
import com.qualiapproche.repository.ActionCorrectivePreventiveRepository;
import com.qualiapproche.repository.ActionExigenceRepository;
import com.qualiapproche.repository.ActionRisqueRepossitory;
import com.qualiapproche.service.ActionCorrectivePreventiveService;
import com.qualiapproche.utils.StatutEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActionCorrectivePreventiveServiceImpl implements ActionCorrectivePreventiveService {
    private final ActionCorrectivePreventiveRepository repository;
    private final ActionMapper actionMapper;
    private final ActionRisqueRepossitory actionRisqueRepossitory;
    private final ActionExigenceRepository actionExigenceRepository;

    @Override
    public ActionCorrectivePreventiveDto createAction(ActionCorrectivePreventiveDto actionCorrectivePreventiveDto) {
        ActionCorrectivePreventive actionCorrectivePreventive = actionMapper.toEntity(actionCorrectivePreventiveDto);
        actionCorrectivePreventive.setStatut(StatutEnum.ACTIF);
        if (!actionCorrectivePreventiveDto.getRisques().isEmpty()) {
            actionCorrectivePreventiveDto.getRisques().forEach(risque -> {
                ActionRisque actionRisque = ActionRisque.builder()
                        .action(repository.save(actionCorrectivePreventive))
                        .risque(risque)
                        .build();
                actionRisqueRepossitory.save(actionRisque);
            });
        }
        if (!actionCorrectivePreventiveDto.getExigences().isEmpty()) {
            actionCorrectivePreventiveDto.getExigences().forEach(exigence -> {
                ActionExisgence actionexigence = ActionExisgence.builder()
                        .action(repository.save(actionCorrectivePreventive))
                        .exigence(exigence)
                        .build();
                actionExigenceRepository.save(actionexigence);
            });
        }
        return actionCorrectivePreventiveDto;
    }

    @Override
    public ActionCorrectivePreventiveDto updateAction(ActionCorrectivePreventiveDto actionCorrectivePreventiveDto) {
        ActionCorrectivePreventive actionCorrectivePreventive = repository.getReferenceById(actionCorrectivePreventiveDto.getId());
        if (Objects.nonNull(actionCorrectivePreventive)) {
            actionMapper.updateEntityFromDto(actionCorrectivePreventiveDto, actionCorrectivePreventive);
            repository.save(actionCorrectivePreventive);
            if (!actionCorrectivePreventiveDto.getRisques().isEmpty()) {
                deleteActionRisque(actionCorrectivePreventive.getId());
                actionCorrectivePreventiveDto.getRisques().forEach(risque -> {
                    ActionRisque actionRisque = ActionRisque.builder()
                            .action(actionCorrectivePreventive)
                            .risque(risque)
                            .build();
                    actionRisqueRepossitory.save(actionRisque);
                });
            }
            if (!actionCorrectivePreventiveDto.getExigences().isEmpty()) {
                deleteActionExigence(actionCorrectivePreventive.getId());
                actionCorrectivePreventiveDto.getExigences().forEach(exigence -> {
                    ActionExisgence actionexigence = ActionExisgence.builder()
                            .action(actionCorrectivePreventive)
                            .exigence(exigence)
                            .build();
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
    public ActionCorrectivePreventiveDto deleteAction(UUID id) {
        ActionCorrectivePreventive actionCorrectivePreventive = repository.getReferenceById(id);
        actionCorrectivePreventive.setStatut(StatutEnum.INACTIF);
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
