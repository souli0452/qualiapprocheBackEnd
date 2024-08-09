package com.qualiapproche.entities;


import jakarta.persistence.Column;
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
public class ActionCorrectivePreventive extends AuditEntity {
    private String libelleActionCorrectivePreventive;
    @Column(columnDefinition = "text",length = 1000000000)
    private String descriptionActionCorrectivePreventive;
    private String responsable;
    private String statut;
    private String typeActionCorrectivePreventive;
    private LocalDateTime dateDebutActionCorrectivePreventive;
    private LocalDateTime dateFinActionCorrectivePreventive;
    @OneToOne
    private Reclamation reclamation;
    @ManyToMany
    private List<Risque> risques;
    @ManyToMany
    private List<Exigence> exigences;
}
