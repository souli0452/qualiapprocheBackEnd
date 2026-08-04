package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.common.dto.DomaineApplicationDto;
import com.qualiapproche.referentiel.entities.DomaineApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DomaineApplicationMapper {

    DomaineApplicationDto toDto(DomaineApplication entite);

    DomaineApplication toEntity(DomaineApplicationDto dto);

    List<DomaineApplicationDto> toDtos(List<DomaineApplication> entites);

    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "updateById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    void updateEntityFromDto(DomaineApplicationDto dto, @MappingTarget DomaineApplication entite);
}
