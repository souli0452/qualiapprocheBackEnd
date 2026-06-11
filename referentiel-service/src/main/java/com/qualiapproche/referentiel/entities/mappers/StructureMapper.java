package com.qualiapproche.referentiel.entities.mappers;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.entities.TypeProcessus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;


@Mapper(componentModel = "spring")
public interface StructureMapper {
    @Mapping(source = "direction.id", target = "directionId")
    @Mapping(source = "direction.libelleLong", target = "libelleDirection")
    @Mapping(source = "typeProcessus.id", target = "typeProcessusId")
    @Mapping(source = "typeProcessus.libelle", target = "typeProcessusLibelle")
    StructureDto toDto(Structure structure);

    @Mapping(source = "directionId", target = "direction")
    @Mapping(source = "typeProcessusId", target = "typeProcessus")
    Structure toEntity(StructureDto structureDto);

    default Structure map(UUID id) {
        if (id == null) {
            return null;
        }
        Structure structure = new Structure();
        structure.setId(id);
        return structure;
    }

    default TypeProcessus mapTypeProcessus(UUID id) {
        if (id == null) {
            return null;
        }
        TypeProcessus typeProcessus = new TypeProcessus();
        typeProcessus.setId(id);
        return typeProcessus;
    }
}
