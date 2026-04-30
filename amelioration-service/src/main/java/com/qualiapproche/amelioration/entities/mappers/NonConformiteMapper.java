package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {ParticipantsMapper.class, PlanActionMapper.class})
public interface NonConformiteMapper extends EntityMapper<NonConformiteDto, NonConformite> {
    void updateEntityFromDto(NonConformiteDto dto, @MappingTarget NonConformite entity);
}
