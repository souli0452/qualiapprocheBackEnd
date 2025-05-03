/*
package com.qualiapproche.entities;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class PlanAction extends AuditEntity{

    private String numeroOdre;
    private String causeIdentifiees;
    private String solutionRetenues;
    private String responsable;
    private String mail;
    private String numeroTelephone;
    private String dateEcheance;
    @ManyToOne
    @JoinColumn(name = "non_conformite_id", nullable = false)
    private NonConformite nonConformite;
}
*/

package com.qualiapproche.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class PlanAction extends AuditEntity {

    private String numeroOdre;
    private String causeIdentifiees;
    private String solutionRetenues;
    private String responsable;
    private String mail;
    private String numeroTelephone;
    private String dateEcheance;

    @ManyToOne
    @JoinColumn(name = "non_conformite_id")
    private NonConformite nonConformite;
}
