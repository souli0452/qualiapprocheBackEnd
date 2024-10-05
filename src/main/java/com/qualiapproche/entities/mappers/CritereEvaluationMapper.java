package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.CrictereEvaluationDto;
import com.qualiapproche.entities.CrictereEvaluation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CritereEvaluationMapper {
    CrictereEvaluationDto toDto(CrictereEvaluation crictereEvaluation);
    CrictereEvaluation toEntity(CrictereEvaluationDto crictereEvaluationDto);
    List<CrictereEvaluation> toDtos(List<CrictereEvaluation> crictereEvaluationList);
    List<CrictereEvaluation> toEntities(List<CrictereEvaluationDto> crictereEvaluationDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(CrictereEvaluationDto enqueteDto, @MappingTarget CrictereEvaluation crictereEvaluation);
    default CrictereEvaluation map(UUID id) {
        if (id == null) {
            return null;
        }
        CrictereEvaluation crictereEvaluation = new CrictereEvaluation();
        crictereEvaluation.setId(id);
        return crictereEvaluation;
    }
}
