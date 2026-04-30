package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.Action;
import com.qualiapproche.common.dto.ActionDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActionMapper extends EntityMapper<ActionDto, Action> {
}
