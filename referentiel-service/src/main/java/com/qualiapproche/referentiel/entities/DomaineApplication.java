package com.qualiapproche.referentiel.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Domaine d'application d'un document, paramétré par l'organisation. */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@Table(name = "domaines_application",
        uniqueConstraints = @UniqueConstraint(name = "uk_domaine_libelle", columnNames = "libelle"))
public class DomaineApplication extends AuditEntity {

    @Column(nullable = false)
    private String libelle;

    private String description;

    /** Rang d'affichage. */
    private Integer ordre;
}
