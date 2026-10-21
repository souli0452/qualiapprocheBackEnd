package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.evaluation.enumeration.NiveauEfficacite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/** Une action prévue pour maîtriser le risque attaché à un audit, et ce qu'elle a donné. */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "action_maitrise_risque")
public class ActionMaitriseRisque extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @Column(name = "libelle", nullable = false, columnDefinition = "TEXT")
    private String libelle;

    @Column(name = "responsable")
    private String responsable;

    @Column(name = "delai")
    private LocalDate delai;

    @Enumerated(EnumType.STRING)
    @Column(name = "efficacite", nullable = false, length = 30)
    private NiveauEfficacite efficacite;

    public void evaluerEfficacite(NiveauEfficacite niveau) {
        this.efficacite = niveau == null ? NiveauEfficacite.NON_EVALUEE : niveau;
    }
}
