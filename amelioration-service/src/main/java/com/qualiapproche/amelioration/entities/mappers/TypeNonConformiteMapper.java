package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.TypeNonConformite;
import com.qualiapproche.common.dto.TypeNonConformiteDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TypeNonConformiteMapper extends EntityMapper<TypeNonConformiteDto, TypeNonConformite> {
}
