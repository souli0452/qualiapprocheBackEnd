package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.CategorieFichierDto;
import com.qualiapproche.entities.CategorieFichier;

@Mapper(componentModel = "spring")
public interface CategorieFichierMapper {
    CategorieFichierDto toDto(CategorieFichier categorieFichier);

    CategorieFichier toEntity(CategorieFichierDto categorieFichierDto);

    List<CategorieFichierDto> toDtos(List<CategorieFichier> categorieFichierList);

    List<CategorieFichier> toEntities(List<CategorieFichierDto> categorieFichierDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(CategorieFichierDto enqueteDto, @MappingTarget CategorieFichier categorieFichier);
    default CategorieFichier map(UUID id) {
        if (id == null) {
            return null;
        }
        CategorieFichier categorieFichier = new CategorieFichier();
        categorieFichier.setId(id);
        return categorieFichier;
    }
}
