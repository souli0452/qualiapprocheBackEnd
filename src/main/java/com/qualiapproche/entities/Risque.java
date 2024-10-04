package com.qualiapproche.entities;

import com.qualiapproche.utils.StatutEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
 @AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Risque extends AuditEntity {

    private String libelle;
    private String description;
    private String niveau;
    private StatutEnum statut;
    private String plantAttenuation;
    private String commentaireRisque;
    private String evidenceRisuqe;
    private LocalDateTime dateIdentificationRisque;
}
