package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.dto.PieceJointeDto;
import com.qualiapproche.entities.PieceJointe;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.PlanActionDto;
import com.qualiapproche.entities.PlanAction;

@Mapper(componentModel = "spring")
public interface PlanActionMapper extends EntityMapper<PlanActionDto, PlanAction>{
    PlanActionDto toDto(PlanAction planAction);
    PlanAction toEntity(PlanActionDto planActionDto);

    List<PlanActionDto> toDtos(List<PlanAction> correctivePreventives);

    List<PlanAction> toEntities(List<PlanActionDto> actionCorrectivePreventiveDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(PlanActionDto planActionDto, @MappingTarget PlanAction planAction);
    default PlanAction map(UUID id) {
        if (id == null) {
            return null;
        }
        PlanAction planAction = new PlanAction();
         planAction.setId(id);
        return planAction;
    }
}
