package com.qualiapproche.dto;

import com.qualiapproche.entities.Fichier;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter @Builder
public class ArchivageDto extends AuditEntityDto{
    private LocalDateTime dateArchivage;
    private Fichier fichier;
}
