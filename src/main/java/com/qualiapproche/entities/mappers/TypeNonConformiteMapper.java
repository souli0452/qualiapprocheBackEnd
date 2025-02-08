package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.TypeNonConformiteDto;
import com.qualiapproche.entities.TypeNonConformite;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TypeNonConformiteMapper {
    TypeNonConformiteDto toDto(TypeNonConformite typeNonConformite);

    TypeNonConformite toEntity(TypeNonConformiteDto typeNonConformiteDto);

    List<TypeNonConformiteDto> toDtos(List<TypeNonConformite> typeNonConformites);

    List<TypeNonConformite> toEntities(List<TypeNonConformiteDto> typeNonConformiteDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
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
