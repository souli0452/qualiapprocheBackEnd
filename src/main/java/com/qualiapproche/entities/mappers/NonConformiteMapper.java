package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.NonConformiteDto;
import com.qualiapproche.entities.NonConformite;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface NonConformiteMapper {
    NonConformiteDto toDto(NonConformite nonConformite);

    NonConformite toEntity(NonConformiteDto nonConformiteDto);

    List<NonConformiteDto> toDtos(List<NonConformite> nonConformiteList);

    List<NonConformite> toEntities(List<NonConformiteDto> nonConformiteDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
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
