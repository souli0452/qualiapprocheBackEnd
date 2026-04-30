package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.Evaluation;
import com.qualiapproche.common.dto.EvaluationDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EvaluationMapper extends EntityMapper<EvaluationDto, Evaluation> {
}
