package com.qualiapproche.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
public class ActionRisque extends   AuditEntity{
    @OneToOne
    private ActionCorrectivePreventive action;
    @OneToOne
    private  Risque risque;
}
