package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.TypeNonConformiteDto;
import com.qualiapproche.entities.TypeNonConformite;

@Mapper(componentModel = "spring")
public interface TypeNonConformiteMapper {
    TypeNonConformiteDto toDto(TypeNonConformite typeNonConformite);

    TypeNonConformite toEntity(TypeNonConformiteDto typeNonConformiteDto);

    List<TypeNonConformiteDto> toDtos(List<TypeNonConformite> typeNonConformites);

    List<TypeNonConformite> toEntities(List<TypeNonConformiteDto> typeNonConformiteDtos);
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "updateById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    void updateEntityFromDto(TypeNonConformiteDto typeNonConformiteDto, @MappingTarget TypeNonConformite typeNonConformite);
    default TypeNonConformite map(UUID id) {
        if (id == null) {
            return null;
        }
        TypeNonConformite typeNonConformite = new TypeNonConformite();
        typeNonConformite.setId(id);
        return typeNonConformite;
    }
}
