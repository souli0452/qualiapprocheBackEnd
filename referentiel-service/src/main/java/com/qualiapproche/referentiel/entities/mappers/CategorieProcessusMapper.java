package com.qualiapproche.referentiel.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.qualiapproche.common.dto.CategorieProcessusDto;
import com.qualiapproche.referentiel.entities.CategorieProcessus;

@Mapper(componentModel = "spring")
public interface CategorieProcessusMapper {
    CategorieProcessusDto toDto(CategorieProcessus categorieProcessus);

    CategorieProcessus toEntity(CategorieProcessusDto categorieProcessusDto);

    List<CategorieProcessusDto> toDtos(List<CategorieProcessus> categorieProcessusses);

    List<CategorieProcessus> toEntities(List<CategorieProcessusDto> categorieProcessusDtos);
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "updateById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    void updateEntityFromDto(CategorieProcessusDto categorieProcessusDto, @MappingTarget CategorieProcessus categorieProcessus);
    default CategorieProcessus map(UUID id) {
        if (id == null) {
            return null;
        }
        CategorieProcessus categorieProcessus = new CategorieProcessus();
        categorieProcessus.setId(id);
        return categorieProcessus;
    }
}
