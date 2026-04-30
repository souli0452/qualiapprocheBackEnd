package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.NiveauNonConformite;
import com.qualiapproche.common.dto.NiveauNonConformiteDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NiveauNonConformiteMapper extends EntityMapper<NiveauNonConformiteDto, NiveauNonConformite> {
}
