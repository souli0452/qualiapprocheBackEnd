package com.qualiapproche.entities;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.utils.StatutEnum;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
public class ActionCorrectivePreventive extends AuditEntity {
    private String libelle;
    @Column(columnDefinition = "text",length = 1000000000)
    private String descriptionActionCorrectivePreventive;
    private String responsable;
    @Enumerated(EnumType.STRING)
    private StatutEnum statut;
    private String type;
    private String dateDebut;
    private String dateFin;
    @OneToOne
    private Reclamation reclamation;
    @ManyToMany
    private List<Risque> risques;
    @ManyToMany
    private List<Exigence> exigences;
}
