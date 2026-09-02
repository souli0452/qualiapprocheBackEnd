package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.SourceDeNonConformite;
import com.qualiapproche.common.dto.SourceNonConformiteDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SourceNonConformiteMapper extends EntityMapper<SourceNonConformiteDto, SourceDeNonConformite> {
}
