package com.qualiapproche.amelioration.service;
import com.qualiapproche.common.dto.ActionDto;
import java.util.List;
import java.util.UUID;

public interface ActionService {
    ActionDto create(ActionDto actionDto);
    ActionDto update(ActionDto actionDto);
    ActionDto getById(UUID id);
    List<ActionDto> getAll();
    void delete(UUID id);
}
