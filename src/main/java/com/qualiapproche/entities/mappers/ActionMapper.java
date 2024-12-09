package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.ActionCorrectivePreventiveDto;
import com.qualiapproche.entities.ActionCorrectivePreventive;

@Mapper(componentModel = "spring")
public interface ActionMapper {
    ActionCorrectivePreventiveDto toDto(ActionCorrectivePreventive actionCorrectivePreventive);

    ActionCorrectivePreventive toEntity(ActionCorrectivePreventiveDto actionCorrectivePreventiveDto);

    List<ActionCorrectivePreventiveDto> toDtos(List<ActionCorrectivePreventive> correctivePreventives);

    List<ActionCorrectivePreventive> toEntities(List<ActionCorrectivePreventiveDto> actionCorrectivePreventiveDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(ActionCorrectivePreventiveDto actionCorrectivePreventiveDto, @MappingTarget ActionCorrectivePreventive actionCorrectivePreventive);
    default ActionCorrectivePreventive map(UUID id) {
        if (id == null) {
            return null;
        }
        ActionCorrectivePreventive actionCorrectivePreventive = new ActionCorrectivePreventive();
         actionCorrectivePreventive.setId(id);
        return actionCorrectivePreventive;
    }
}
