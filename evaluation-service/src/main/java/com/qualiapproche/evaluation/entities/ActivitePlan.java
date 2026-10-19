package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import java.time.LocalDate;
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

/** Une ligne du déroulé d'un plan d'audit : un créneau, une activité, qui audite qui. */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "activite_plan")
public class ActivitePlan extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_audit_id", nullable = false)
    private PlanAudit plan;

    /** Le jour de l'activité, dans la période de l'audit ; nul sur les plans antérieurs. */
    @Column(name = "jour")
    private LocalDate jour;

    /** « 08:30 » : l'heure, que le jour complète. */
    @Column(name = "horaire_debut", length = 30)
    private String horaireDebut;

    @Column(name = "horaire_fin", length = 30)
    private String horaireFin;

    @Column(name = "libelle_activite", nullable = false, columnDefinition = "TEXT")
    private String libelleActivite;

    @Column(name = "processus_concerne")
    private String processusConcerne;

    @Column(name = "auditeur")
    private String auditeur;

    @Column(name = "personnes_auditees", columnDefinition = "TEXT")
    private String personnesAuditees;
}
