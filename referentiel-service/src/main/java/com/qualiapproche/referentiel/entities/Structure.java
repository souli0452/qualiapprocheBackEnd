package com.qualiapproche.referentiel.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "structures")
@JsonInclude(NON_NULL)
public class Structure extends AuditEntity {
    private String libelleLong;
    private String libelleCourt;
    private String emailStruct;
    private String telephone;
    private String adresse;
    private String responsable;
    private String titreHonorifiqueSignataire;
    private String email;
    private String ville;
    private String region;
    private String autoriteSignataire;
    private String titreAutoriteSignataire;
    private String titreSignataire;
    @Enumerated(EnumType.STRING)
    private TypeStructure typeStructure;
    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "parent_direction_id")
    private Structure direction;

    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "type_processus_id")
    private TypeProcessus typeProcessus;

    private Boolean licenceActive;
}
