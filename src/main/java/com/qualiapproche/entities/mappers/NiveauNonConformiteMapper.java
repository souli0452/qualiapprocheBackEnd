package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.NiveauNonConformiteDto;
import com.qualiapproche.entities.NiveauNonConformite;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface NiveauNonConformiteMapper {
    NiveauNonConformiteDto toDto(NiveauNonConformite niveauNonConformite);

    NiveauNonConformite toEntity(NiveauNonConformiteDto niveauNonConformiteDto);

    List<NiveauNonConformiteDto> toDtos(List<NiveauNonConformite> niveauNonConformites);

    List<NiveauNonConformite> toEntities(List<NiveauNonConformiteDto> niveauNonConformiteDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
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
