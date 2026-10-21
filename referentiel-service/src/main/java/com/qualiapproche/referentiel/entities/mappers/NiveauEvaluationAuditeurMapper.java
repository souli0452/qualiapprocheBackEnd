package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.referentiel.entities.NiveauEvaluationAuditeur;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NiveauEvaluationAuditeurMapper {

    NiveauEvaluationAuditeurDto toDto(NiveauEvaluationAuditeur entite);
}
