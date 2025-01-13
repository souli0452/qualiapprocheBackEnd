package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.ExigenceDto;
import com.qualiapproche.entities.Exigence;

@Mapper(componentModel = "spring")
public interface ExigenceMapper {
    ExigenceDto toDto(Exigence exigence);

    Exigence toEntity(ExigenceDto exigenceDto);

    List<ExigenceDto> toDtos(List<Exigence> exigenceList);

    List<Exigence> toEntities(List<ExigenceDto> exigenceDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(ExigenceDto enqueteDto, @MappingTarget Exigence exigence);
    default Exigence map(UUID id) {
        if (id == null) {
            return null;
        }
        Exigence exigence = new Exigence();
        exigence.setId(id);
        return exigence;
    }
}
