package com.qualiapproche.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Risque extends AuditEntity {

    private String libelleRisque;
    private String descriptionRisque;
    private String niveauRisque;
    private String statutRisque;
    private String plantAttenuation;
    private String commentaireRisque;
    private String evidenceRisuqe;
    private LocalDateTime dateIdentificationRisque;
    @OneToOne
    private Risque risque;
    @ManyToMany
    private List<ActionCorrectivePreventive> actionCorrectivePreventives;

}
