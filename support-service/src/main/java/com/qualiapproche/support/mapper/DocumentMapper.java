package com.qualiapproche.support.mapper;

import com.qualiapproche.support.dto.DocumentQmsDto;
import com.qualiapproche.support.model.DocumentQms;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {WorkflowMapper.class}, unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface DocumentMapper {

    @Mapping(target = "status", source = ".", qualifiedByName = "mapStatus")
    DocumentQmsDto toDto(DocumentQms doc);

    DocumentQms toEntity(DocumentQmsDto dto);

    @Named("mapStatus")
    default String mapStatus(DocumentQms doc) {
        if (doc == null) return null;
        if (doc.isArchived()) return "archive";
        if (doc.isObsolete()) return "obsolete";
        if (doc.isEsTraiter()) return "valide";
        if (doc.getCurrentStep() != null) return "en_approbation";
        return "brouillon";
    }
}
