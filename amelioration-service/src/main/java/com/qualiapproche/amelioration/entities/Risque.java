package com.qualiapproche.amelioration.entities;
import com.qualiapproche.common.base.AuditEntity;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Risque extends AuditEntity {

    private String libelle;
    private String description;
    private String niveau;
    private StatutRisque statut;
    private String plantAttenuation;
    private String commentaireRisque;
    private String evidenceRisque;
    // @lombok.Getter @lombok.Setter private LocalDateTime dateIdentificationRisque;

    public enum StatutRisque {
        STATUS_1,
        STATUS_2,
        STATUS_3,
    }
}
