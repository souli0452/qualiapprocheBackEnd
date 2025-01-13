package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.ReclamationDto;
import com.qualiapproche.entities.Reclamation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ReclamationMapper {
    ReclamationDto toDto(Reclamation reclamation);

    Reclamation toEntity(ReclamationDto reclamationDto);

    List<ReclamationDto> toDtos(List<Reclamation> reclamationList);

    List<Reclamation> toEntities(List<ReclamationDto> reclamationDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(ReclamationDto enqueteDto, @MappingTarget Reclamation reclamation);
    default Reclamation map(UUID id) {
        if (id == null) {
            return null;
        }
        Reclamation reclamation = new Reclamation();
        reclamation.setId(id);
        return reclamation;
    }
}
