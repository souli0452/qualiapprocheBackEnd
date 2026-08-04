package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.common.dto.NiveauConfidentialiteDto;
import com.qualiapproche.referentiel.entities.NiveauConfidentialite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NiveauConfidentialiteMapper {

    NiveauConfidentialiteDto toDto(NiveauConfidentialite entite);

    NiveauConfidentialite toEntity(NiveauConfidentialiteDto dto);

    List<NiveauConfidentialiteDto> toDtos(List<NiveauConfidentialite> entites);

    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "updateById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    void updateEntityFromDto(NiveauConfidentialiteDto dto, @MappingTarget NiveauConfidentialite entite);
}
