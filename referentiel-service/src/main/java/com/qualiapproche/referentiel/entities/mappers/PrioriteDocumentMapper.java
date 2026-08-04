package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.common.dto.PrioriteDocumentDto;
import com.qualiapproche.referentiel.entities.PrioriteDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PrioriteDocumentMapper {

    PrioriteDocumentDto toDto(PrioriteDocument entite);

    PrioriteDocument toEntity(PrioriteDocumentDto dto);

    List<PrioriteDocumentDto> toDtos(List<PrioriteDocument> entites);

    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "updateById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    void updateEntityFromDto(PrioriteDocumentDto dto, @MappingTarget PrioriteDocument entite);
}
