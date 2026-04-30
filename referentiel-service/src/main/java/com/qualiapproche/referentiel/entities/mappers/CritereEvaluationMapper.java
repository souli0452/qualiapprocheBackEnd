package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.common.dto.CrictereEvaluationDto;
import com.qualiapproche.referentiel.entities.CrictereEvaluation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CritereEvaluationMapper {
    @Mapping(source = "fournisseur.id", target = "fournisseurId")
    CrictereEvaluationDto toDto(CrictereEvaluation entity);

    @Mapping(source = "fournisseurId", target = "fournisseur.id")
    CrictereEvaluation toEntity(CrictereEvaluationDto dto);

    List<CrictereEvaluationDto> toDtos(List<CrictereEvaluation> entities);
    List<CrictereEvaluation> toEntities(List<CrictereEvaluationDto> dtos);
}
