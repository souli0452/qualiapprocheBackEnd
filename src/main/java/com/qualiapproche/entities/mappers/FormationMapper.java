package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.FormationDto;
import com.qualiapproche.entities.Formation;

@Mapper(componentModel = "spring")
public interface FormationMapper {
    FormationDto toDto(Formation formation);

    Formation toEntity(FormationDto formationDto);

    List<FormationDto> toDtos(List<Formation> formations);

    List<Formation> toEntities(List<FormationDto> formationDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(FormationDto formationDto, @MappingTarget Formation formation);
    default Formation map(UUID id) {
        if (id == null) {
            return null;
        }
        Formation formation = new Formation();
        // formation.setId(id);
        return formation;
    }
}
