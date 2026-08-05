package com.qualiapproche.support.mapper;

import com.qualiapproche.support.dto.DocumentQmsDto;
import com.qualiapproche.support.model.DocumentQms;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface DocumentMapper {

    DocumentQmsDto toDto(DocumentQms doc);

    DocumentQms toEntity(DocumentQmsDto dto);
}

