package com.qualiapproche.service;

import com.qualiapproche.dto.ActionCorrectivePreventiveDto;

import java.util.List;
import java.util.UUID;

public interface ActionCorrectivePreventiveService {
    ActionCorrectivePreventiveDto createAction(ActionCorrectivePreventiveDto actionCorrectivePreventiveDto);
    ActionCorrectivePreventiveDto updateAction(ActionCorrectivePreventiveDto actionCorrectivePreventiveDto);
    ActionCorrectivePreventiveDto getActionById(UUID id);
    List<ActionCorrectivePreventiveDto> getAlls();
    ActionCorrectivePreventiveDto deleteAction(UUID id);

}
