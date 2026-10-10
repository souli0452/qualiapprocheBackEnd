package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.common.dto.SiteDto;
import com.qualiapproche.referentiel.entities.Site;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SiteMapper {

    SiteDto toDto(Site entite);
}
