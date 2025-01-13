package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.AuditeDto;
import com.qualiapproche.entities.Audite;

@Mapper(componentModel = "spring")
public interface AuditeMapper {
    AuditeDto toDto(Audite Audite);

    Audite toEntity(AuditeDto AuditeDto);

    List<AuditeDto> toDtos(List<Audite> Audites);

    List<Audite> toEntities(List<AuditeDto> AuditeDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(AuditeDto AuditeDto, @MappingTarget Audite Audite);
    default Audite map(UUID id) {
        if (id == null) {
            return null;
        }
        Audite Audite = new Audite();
        Audite.setId(id);
        return Audite;
    }
}
