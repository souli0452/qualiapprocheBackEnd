package com.qualiapproche.referentiel.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Niveau de priorité d'un document, paramétré par l'organisation. */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@Table(name = "priorites_document",
        uniqueConstraints = @UniqueConstraint(name = "uk_priorite_libelle", columnNames = "libelle"))
public class PrioriteDocument extends AuditEntity {

    @Column(nullable = false)
    private String libelle;

    private String description;

    /** Rang d'affichage, du plus urgent au moins urgent. */
    private Integer ordre;

    /** Couleur d'affichage, facultative. */
    private String couleur;
}
