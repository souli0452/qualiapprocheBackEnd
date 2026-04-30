package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.Reclamation;
import com.qualiapproche.common.dto.ReclamationDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReclamationMapper extends EntityMapper<ReclamationDto, Reclamation> {
}
