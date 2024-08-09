package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class Archivage extends AuditEntity  {
    private LocalDateTime dateArchivage;
    @OneToOne
    private Fichier fichier;
}
