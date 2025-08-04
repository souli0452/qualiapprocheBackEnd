package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.NonConformiteDto;
import com.qualiapproche.entities.NonConformite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface NonConformiteMapper {
    @Mapping(source = "participants.fullNames", target = "participants")
    NonConformiteDto toDto(NonConformite nonConformite);
    @Mapping(source = "participants", target = "participants.fullNames")
    NonConformite toEntity(NonConformiteDto nonConformiteDto);
    @Mapping(source = "participants.fullNames", target = "participants")
    List<NonConformiteDto> toDtos(List<NonConformite> nonConformiteList);
    @Mapping(source = "participants", target = "participants.fullNames")
    List<NonConformite> toEntities(List<NonConformiteDto> nonConformiteDtos);
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "updateById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(source = "participants", target = "participants.fullNames")
    void updateEntityFromDto(NonConformiteDto enqueteDto, @MappingTarget NonConformite nonConformite);
    default NonConformite map(UUID id) {
        if (id == null) {
            return null;
        }
        NonConformite nonConformite = new NonConformite();
        nonConformite.setId(id);
        return nonConformite;
    }
}
