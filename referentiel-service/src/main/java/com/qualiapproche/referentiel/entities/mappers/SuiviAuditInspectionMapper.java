package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.referentiel.entities.SuiviAuditInspection;
import com.qualiapproche.common.dto.SuiviAuditInspectionDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SuiviAuditInspectionMapper extends EntityMapper<SuiviAuditInspectionDto, SuiviAuditInspection> {
}
