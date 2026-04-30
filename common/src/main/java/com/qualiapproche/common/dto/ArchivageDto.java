package com.qualiapproche.common.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ArchivageDto extends AuditEntityDto {
    private LocalDateTime dateArchivage;
}
