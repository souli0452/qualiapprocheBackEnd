package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.ReglementationDto;
import com.qualiapproche.entities.Reglementation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ReglementationMapper {
    ReglementationDto toDto(Reglementation reglementation);

    Reglementation toEntity(ReglementationDto reglementationDto);

    List<ReglementationDto> toDtos(List<Reglementation> reglementationList);

    List<Reglementation> toEntities(List<ReglementationDto> reglementationDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(ReglementationDto enqueteDto, @MappingTarget Reglementation reglementation);
    default Reglementation map(UUID id) {
        if (id == null) {
            return null;
        }
        Reglementation reglementation = new Reglementation();
        reglementation.setId(id);
        return reglementation;
    }
}
