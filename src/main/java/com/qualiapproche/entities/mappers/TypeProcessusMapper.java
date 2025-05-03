package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.TypeProcessusDto;
import com.qualiapproche.entities.TypeProcessus;

@Mapper(componentModel = "spring")
public interface TypeProcessusMapper {
    TypeProcessusDto toDto(TypeProcessus typeProcessus);

    TypeProcessus toEntity(TypeProcessusDto typeProcessusDto);

    List<TypeProcessusDto> toDtos(List<TypeProcessus> typeProcessuss);

    List<TypeProcessus> toEntities(List<TypeProcessusDto> typeProcessusDtos);
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "updateById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
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
