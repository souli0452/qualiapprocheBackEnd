package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.NiveauNonConformiteDto;
import com.qualiapproche.entities.NiveauNonConformite;

@Mapper(componentModel = "spring")
public interface NiveauNonConformiteMapper {
    NiveauNonConformiteDto toDto(NiveauNonConformite niveauNonConformite);

    NiveauNonConformite toEntity(NiveauNonConformiteDto niveauNonConformiteDto);

    List<NiveauNonConformiteDto> toDtos(List<NiveauNonConformite> niveauNonConformites);

    List<NiveauNonConformite> toEntities(List<NiveauNonConformiteDto> niveauNonConformiteDtos);
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "updateById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    void updateEntityFromDto(NiveauNonConformiteDto niveauNonConformiteDto, @MappingTarget NiveauNonConformite niveauNonConformite);
    default NiveauNonConformite map(UUID id) {
        if (id == null) {
            return null;
        }
        NiveauNonConformite niveauNonConformite = new NiveauNonConformite();
        niveauNonConformite.setId(id);
        return niveauNonConformite;
    }
}
