package com.qualiapproche.amelioration.entities;
import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.utils.StatutEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
