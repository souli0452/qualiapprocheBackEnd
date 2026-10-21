package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.common.dto.CritereEvaluationAuditeurDto;
import com.qualiapproche.referentiel.entities.CritereEvaluationAuditeur;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CritereEvaluationAuditeurMapper {

    CritereEvaluationAuditeurDto toDto(CritereEvaluationAuditeur entite);
}
