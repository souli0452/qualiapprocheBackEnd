package com.qualiapproche.workflow.service;

import com.qualiapproche.common.dto.EmailTemplateDto;
import com.qualiapproche.workflow.model.EmailTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EmailTemplateMapper {

    public EmailTemplateDto toDto(EmailTemplate entity) {
        if (entity == null) {
            return null;
        }
        return EmailTemplateDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .description(entity.getDescription())
                .build();
    }

    public EmailTemplate toEntity(EmailTemplateDto dto) {
        if (dto == null) {
            return null;
        }
        return EmailTemplate.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .subject(dto.getSubject())
                .body(dto.getBody())
                .description(dto.getDescription())
                .build();
    }

    public List<EmailTemplateDto> toDtoList(List<EmailTemplate> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
