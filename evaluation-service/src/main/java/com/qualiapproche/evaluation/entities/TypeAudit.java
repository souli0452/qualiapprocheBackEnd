package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Le type d'un audit : interne, fournisseur, de certification...
 *
 * <p>{@code circuitExterne} dit qui signe le rapport. Faux, le rapport passe par le circuit de
 * signatures de l'application avant que l'audit se clôture ; vrai, il est signé hors de
 * l'application — par un organisme certificateur, typiquement — et c'est le rapport chargé qui
 * en fait foi.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "type_audit")
public class TypeAudit extends AuditEntity {

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "circuit_externe", nullable = false)
    private boolean circuitExterne;

    /**
     * L'origine que porte, dans le module Non-conformités, un écart relevé par ce type d'audit —
     * « Audit interne », « Audit de certification ». Facultative, comme la gravité d'un constat.
     */
    @Column(name = "type_non_conformite_id")
    private UUID typeNonConformiteId;
}
