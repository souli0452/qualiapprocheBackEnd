package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateDto {
    private UUID id;
    private String code;
    private String subject;
    private String body;
    private String description;
}
