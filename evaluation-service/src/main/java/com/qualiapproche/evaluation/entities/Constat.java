package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
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
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ce qu'un auditeur a relevé pendant l'audit.
 *
 * <p>Trois paliers, voir {@link StatutConstat}. Le {@code redacteur} est l'auteur réel de la
 * saisie ; il peut différer de l'auditeur assigné au point de contrôle dont le constat est issu,
 * quand un collègue le relève à sa place.</p>
 *
 * <p>{@code dateSaisie} et {@code commentaireEquipe} servent la compilation d'équipe : la revue
 * collective qui précède la validation.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "constat")
public class Constat extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @Column(name = "critere_audit", columnDefinition = "TEXT")
    private String critereAudit;

    @Column(name = "observation", nullable = false, columnDefinition = "TEXT")
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutConstat statut;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @Column(name = "date_saisie", nullable = false)
    private LocalDateTime dateSaisie;

    @Column(name = "commentaire_equipe", columnDefinition = "TEXT")
    private String commentaireEquipe;

    /** Sa nature : point fort, piste d'amélioration, non-conformité... */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_constat_id")
    private TypeConstat nature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id")
    private ChecklistAudit checklist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_controle_id")
    private PointControle pointControle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "redacteur_id", nullable = false)
    private Auditeur redacteur;

    /** La non-conformité ouverte à partir de ce constat, dans amelioration-service. */
    @Column(name = "non_conformite_id")
    private UUID nonConformiteId;

    /** Son auteur le verse à la compilation d'équipe. */
    public void publier() {
        exigerStatut(StatutConstat.BROUILLON, "versé à la compilation d'équipe");
        if (nature == null) {
            throw new BusinessException("Un constat se verse à l'équipe avec sa nature : celle-ci"
                    + " décide s'il deviendra une non-conformité.", HttpStatus.CONFLICT);
        }
        statut = StatutConstat.COMPILE;
    }

    /** L'équipe le renvoie à son auteur pour retravail. */
    public void retirerVersBrouillon() {
        exigerStatut(StatutConstat.COMPILE, "renvoyé en brouillon");
        statut = StatutConstat.BROUILLON;
    }

    /** Validé en réunion d'équipe : verrouillé, il alimente le rapport et la transmission. */
    public void valider() {
        exigerStatut(StatutConstat.COMPILE, "validé");
        statut = StatutConstat.PUBLIE;
        datePublication = LocalDateTime.now();
    }

    public boolean estNonConformite() {
        return nature != null && nature.isGenereNonConformite();
    }

    private void exigerStatut(StatutConstat attendu, String geste) {
        if (statut != attendu) {
            throw new BusinessException("Un constat " + statut + " ne peut pas être " + geste
                    + " : il faut qu'il soit " + attendu + ".", HttpStatus.CONFLICT);
        }
    }
}
