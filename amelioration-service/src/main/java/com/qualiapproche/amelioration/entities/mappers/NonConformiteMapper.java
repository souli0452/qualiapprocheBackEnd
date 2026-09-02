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
    @Mapping(target = "numeroDeReference", source = "numeroDeReference")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "etatDeTraitement", source = "etatDeTraitement")
    @Mapping(target = "structureDeSoumissionId", source = "structureDeSoumissionId")
    @Mapping(target = "structureDeSoumissionLibelle", source = "structureDeSoumissionLibelle")
    @Mapping(target = "sourceDeNonConformiteId", source = "sourceDeNonConformiteId")
    @Mapping(target = "sourceDeNonConformiteLibelle", source = "sourceDeNonConformiteLibelle")
    @Mapping(target = "categorieProcessusId", source = "categorieProcessusId")
    @Mapping(target = "categorieProcessusLibelle", source = "categorieProcessusLibelle")
    @Mapping(target = "agentImputeId", source = "agentImputeId")
    @Mapping(target = "agentImputeNomComplet", source = "agentImputeNomComplet")
    @Mapping(target = "agentImputeEmail", source = "agentImputeEmail")
    @Mapping(target = "actionImmediate", source = "actionImmediate")
    @Mapping(target = "pertinencePilote", source = "pertinencePilote")
    @Mapping(target = "pertinenceRs", source = "pertinenceRs")
    @Mapping(target = "currentUserStructure", source = "structureDeSoumissionLibelle")
    NonConformiteDto toDto(NonConformite entity);

    @Override
    @Mapping(target = "fichiers", ignore = true)
    @Mapping(target = "docRejet", ignore = true)
    NonConformite toEntity(NonConformiteDto dto);

    @Override
    @Mapping(target = "fichiers", ignore = true)
    @Mapping(target = "docRejet", ignore = true)
    @Mapping(target = "planActions", ignore = true)
    void updateEntityFromDto(NonConformiteDto dto, @MappingTarget NonConformite entity);
}
