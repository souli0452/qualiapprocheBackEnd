package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PlanActionMapper extends EntityMapper<PlanActionDto, PlanAction> {

    @Override
    @Mapping(target = "numeroOrdre", source = "numeroOrdre")
    @Mapping(target = "nonConformiteId", source = "nonConformiteId")
    @Mapping(target = "causeIdentifiee", source = "causeIdentifiee")
    @Mapping(target = "solutionRetenue", source = "solutionRetenue")
    @Mapping(target = "numeroOdre", source = "numeroOrdre")
    @Mapping(target = "nonConformeId", source = "nonConformiteId")
    @Mapping(target = "causeIdentifiees", source = "causeIdentifiee")
    @Mapping(target = "solutionRetenues", source = "solutionRetenue")
    PlanActionDto toDto(PlanAction entity);

    @Override
    void updateEntityFromDto(PlanActionDto dto, @MappingTarget PlanAction entity);
}
