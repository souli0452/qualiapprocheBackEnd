package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.TypeProcessusDto;
import com.qualiapproche.entities.TypeProcessus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TypeProcessusMapper {
    TypeProcessusDto toDto(TypeProcessus typeProcessus);

    TypeProcessus toEntity(TypeProcessusDto typeProcessusDto);

    List<TypeProcessusDto> toDtos(List<TypeProcessus> typeProcessuss);

    List<TypeProcessus> toEntities(List<TypeProcessusDto> typeProcessusDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(TypeProcessusDto typeProcessusDto, @MappingTarget TypeProcessus typeProcessus);
    default TypeProcessus map(UUID id) {
        if (id == null) {
            return null;
        }
        TypeProcessus typeProcessus = new TypeProcessus();
        typeProcessus.setId(id);
        return typeProcessus;
    }
}
