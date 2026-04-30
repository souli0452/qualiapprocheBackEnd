package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.ActionCorrectivePreventive;
import com.qualiapproche.common.dto.ActionCorrectivePreventiveDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActionCorrectivePreventiveMapper extends EntityMapper<ActionCorrectivePreventiveDto, ActionCorrectivePreventive> {
}
