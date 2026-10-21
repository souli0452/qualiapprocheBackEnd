package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Une question de la checklist, et la preuve qu'on attend pour y répondre. */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "point_controle")
public class PointControle extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    private ChecklistAudit checklist;

    @Column(name = "chapitre_iso", length = 50)
    private String chapitreISO;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    @Column(name = "preuve_attendue", columnDefinition = "TEXT")
    private String preuveAttendue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditeur_assigne_id")
    private Auditeur auditeurAssigne;
}
