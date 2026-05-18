package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = { ParticipantsMapper.class, PlanActionMapper.class })
public interface NonConformiteMapper extends EntityMapper<NonConformiteDto, NonConformite> {

    @Override
    @Mapping(target = "fichiers", ignore = true)
    @Mapping(target = "docRejet", ignore = true)
    NonConformite toEntity(NonConformiteDto dto);

    @Override
    @Mapping(target = "fichiers", ignore = true)
    @Mapping(target = "docRejet", ignore = true)
    void updateEntityFromDto(NonConformiteDto dto, @MappingTarget NonConformite entity);
}
