package com.qualiapproche.referentiel.entities;
import com.qualiapproche.common.base.AuditEntity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.common.utils.StatutEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
public class Formation extends AuditEntity {
    private String libelleFormation;
    private String descriptionFormation;

    @Enumerated(EnumType.STRING)
    private StatutEnum statut;
}
