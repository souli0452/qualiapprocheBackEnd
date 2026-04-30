package com.qualiapproche.referentiel.entities;
import com.qualiapproche.common.base.AuditEntity;


import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;

import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@SuperBuilder
public class Archivage extends AuditEntity  {
    private LocalDateTime dateArchivage;
   /* @OneToOne
    private Fichier fichier;*/
}
