package com.qualiapproche.amelioration.entities;

import com.qualiapproche.referentiel.entities.Exigence;
import com.qualiapproche.common.base.AuditEntity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Entity
@JsonInclude(NON_NULL)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ActionExisgence extends AuditEntity {
    @OneToOne
    private ActionCorrectivePreventive action;
    @Column(name = "exigence_id")
    private UUID exigenceId;
}
