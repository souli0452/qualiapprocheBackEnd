package com.qualiapproche.amelioration.entities;

import com.qualiapproche.common.base.AuditEntity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Entity
@JsonInclude(NON_NULL)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ActionRisque extends AuditEntity {
    @OneToOne
    private ActionCorrectivePreventive action;
    @OneToOne
    private Risque risque;
}
