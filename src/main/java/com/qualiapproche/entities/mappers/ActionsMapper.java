package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.ActionDto;
import com.qualiapproche.entities.Action;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ActionsMapper {
    ActionDto toDto(Action action);

    Action toEntity(ActionDto actionDto);

    List<ActionDto> toDtos(List<Action> actions);

    List<Action> toEntities(List<ActionDto> actionDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(ActionDto actionDto, @MappingTarget Action action);
    default Action map(UUID id) {
        if (id == null) {
            return null;
        }
        Action action = new Action();
        action.setId(id);
        return action;
    }
}
