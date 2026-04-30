package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PlanActionMapper extends EntityMapper<PlanActionDto, PlanAction> {
    void updateEntityFromDto(PlanActionDto dto, @MappingTarget PlanAction entity);
}
