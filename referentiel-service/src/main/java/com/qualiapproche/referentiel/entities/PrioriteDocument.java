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

    /**
     * Poids de la priorité, du plus urgent au moins urgent.
     *
     * <p>C'est lui qui range les priorités entre elles. Le libellé ne le permet pas : « Normal » et
     * « Urgent » se suivent dans l'ordre alphabétique en sens inverse de leur urgence. L'échelle
     * appartient à l'organisation, qui décide de son étendue.</p>
     */
    private Integer score;

    /** Couleur d'affichage, facultative. */
    private String couleur;
}
