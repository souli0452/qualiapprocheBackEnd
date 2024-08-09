package com.qualiapproche.dto;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ReclamationDto extends AuditEntityDto{
    private String numeroReference;
    private String nomDemendeur;
    private String dateReclamation;
    private LocalDateTime dateModificationReclamation;
}
