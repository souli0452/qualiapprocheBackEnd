package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.Risque;
import com.qualiapproche.common.dto.RisqueDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RisqueMapper extends EntityMapper<RisqueDto, Risque> {
}
