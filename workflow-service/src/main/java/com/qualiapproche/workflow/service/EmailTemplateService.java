package com.qualiapproche.workflow.service;

import com.qualiapproche.common.dto.EmailTemplateDto;
import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailTemplateMapper emailTemplateMapper;

    @Transactional(readOnly = true)
    public List<EmailTemplateDto> getAllTemplates() {
        return emailTemplateMapper.toDtoList(emailTemplateRepository.findAll());
    }

    @Transactional(readOnly = true)
    public EmailTemplateDto getTemplateById(UUID id) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email template not found for ID: " + id));
        return emailTemplateMapper.toDto(template);
    }

    @Transactional
    public EmailTemplateDto createTemplate(EmailTemplateDto dto) {
        if (emailTemplateRepository.findByCode(dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Email template with code '" + dto.getCode() + "' already exists.");
        }
        EmailTemplate template = emailTemplateMapper.toEntity(dto);
        template = emailTemplateRepository.save(template);
        return emailTemplateMapper.toDto(template);
    }

    @Transactional
    public EmailTemplateDto updateTemplate(UUID id, EmailTemplateDto dto) {
        EmailTemplate existing = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email template not found for ID: " + id));
        
        // check code uniqueness
        if (!existing.getCode().equals(dto.getCode()) && emailTemplateRepository.findByCode(dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Email template with code '" + dto.getCode() + "' already exists.");
        }

        existing.setCode(dto.getCode());
        existing.setSubject(dto.getSubject());
        existing.setBody(dto.getBody());
        existing.setDescription(dto.getDescription());
        
        existing = emailTemplateRepository.save(existing);
        return emailTemplateMapper.toDto(existing);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        if (!emailTemplateRepository.existsById(id)) {
            throw new IllegalArgumentException("Email template not found for ID: " + id);
        }
        emailTemplateRepository.deleteById(id);
    }
}
