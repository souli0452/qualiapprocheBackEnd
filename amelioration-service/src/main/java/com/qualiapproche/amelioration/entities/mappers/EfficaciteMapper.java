package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.Efficacite;
import com.qualiapproche.common.dto.EfficaciteDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EfficaciteMapper extends EntityMapper<EfficaciteDto, Efficacite> {
}
