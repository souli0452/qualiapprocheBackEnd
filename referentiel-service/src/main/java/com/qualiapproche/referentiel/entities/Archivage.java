package com.qualiapproche.referentiel.entities;
import com.qualiapproche.common.base.AuditEntity;


import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
