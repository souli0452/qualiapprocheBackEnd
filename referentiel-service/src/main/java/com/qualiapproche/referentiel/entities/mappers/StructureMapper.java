package com.qualiapproche.referentiel.entities.mappers;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.referentiel.entities.Structure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;


@Mapper(componentModel = "spring")
public interface StructureMapper {
    @Mapping(source = "direction.id", target = "directionId")
    @Mapping(source = "direction.libelleLong", target = "libelleDirection")
    StructureDto toDto(Structure structure);

    @Mapping(source = "directionId", target = "direction")
    Structure toEntity(StructureDto structureDto);

    default Structure map(UUID id) {
        if (id == null) {
            return null;
        }
        Structure structure = new Structure();
        structure.setId(id);
        return structure;
    }
}
