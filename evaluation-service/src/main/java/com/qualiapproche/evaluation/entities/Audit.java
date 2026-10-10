package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.enumeration.NiveauRisque;
import com.qualiapproche.evaluation.enumeration.OrigineDocument;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Un audit du programme : ce qu'on audite, quand, par qui, et ce qui en sort.
 *
 * <p>Les transitions de statut vivent ici, et nulle part ailleurs : un service qui changerait le
 * statut à la main pourrait sauter une étape que l'entité, elle, refuse. Les conditions qui
 * demandent d'autres données — constats en suspens, circuit de signature — sont vérifiées par le
 * service avant d'appeler {@link #cloturer()}.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "audit")
public class Audit extends AuditEntity {

    @Column(name = "reference", nullable = false, length = 60)
    private String reference;

    @Column(name = "objectifs_audit", columnDefinition = "TEXT")
    private String objectifsAudit;

    @Column(name = "portee_audit", columnDefinition = "TEXT")
    private String porteeAudit;

    @Column(name = "criteres_audit", columnDefinition = "TEXT")
    private String criteresAudit;

    @Column(name = "date_debut_prevue")
    private LocalDate dateDebutPrevue;

    @Column(name = "date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Column(name = "duree_estimee_jours")
    private Integer dureeEstimeeJours;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_risque", length = 50)
    private NiveauRisque niveauRisque;

    @Column(name = "description_risque", columnDefinition = "TEXT")
    private String descriptionRisque;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAudit statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "origine_rapport", length = 20)
    private OrigineDocument origineRapport;

    /** La référence du rapport — « ABMAQ-RAP-2026-010 » —, attribuée à son envoi en signature. */
    @Column(name = "reference_rapport", length = 60)
    private String referenceRapport;

    /** Les conclusions et recommandations du rapport : la part rédigée par l'équipe. */
    @Column(name = "conclusions_rapport", columnDefinition = "TEXT")
    private String conclusionsRapport;

    @Column(name = "recommandations_rapport", columnDefinition = "TEXT")
    private String recommandationsRapport;

    /**
     * L'organisme qui mène un audit externe — « Bureau Veritas ». Le responsable d'équipe est
     * alors l'interlocuteur interne de l'organisme.
     */
    @Column(name = "organisme_externe")
    private String organismeExterne;

    /** Les référentiels audités : « ISO 9001 », « Grille RQAP-BF »... */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "audit_referentiel", joinColumns = @JoinColumn(name = "audit_id"))
    @Column(name = "valeur", nullable = false)
    private List<String> referentiels = new ArrayList<>();

    /** Les domaines du référentiel RQAP-BF concernés : des racines de l'arbre. */
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "audit_domaine_rqapbf",
            joinColumns = @JoinColumn(name = "audit_id"),
            inverseJoinColumns = @JoinColumn(name = "noeud_id"))
    private Set<NoeudReferentiel> domainesRqapbf = new LinkedHashSet<>();

    /** Référence du rapport sur le serveur de fichiers, jamais une adresse publique. */
    @Column(name = "fichier_rapport", columnDefinition = "TEXT")
    private String fichierRapport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_audit_id")
    private TypeAudit type;

    /** Le processus audité : une structure du référentiel, vivant dans referentiel-service. */
    @Column(name = "processus_id")
    private UUID processusId;

    /** Les sites concernés : paramétrés dans referentiel-service, ils ne sont ici que des identifiants. */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "audit_site", joinColumns = @JoinColumn(name = "audit_id"))
    @Column(name = "site_id", nullable = false)
    private Set<UUID> siteIds = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActionMaitriseRisque> actionsMaitriseRisques = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_equipe_id")
    private Auditeur responsableEquipe;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "audit_membre_equipe",
            joinColumns = @JoinColumn(name = "audit_id"),
            inverseJoinColumns = @JoinColumn(name = "auditeur_id"))
    private Set<Auditeur> membresEquipe = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "audit_checklist",
            joinColumns = @JoinColumn(name = "audit_id"),
            inverseJoinColumns = @JoinColumn(name = "checklist_id"))
    private Set<ChecklistAudit> checklists = new LinkedHashSet<>();

    /** Le programme est arrêté : l'audit passe en préparation de son plan. */
    public void valider() {
        exigerStatut("valider", StatutAudit.PLANIFIE);
        exigerDatesEtResponsable();
        statut = StatutAudit.EN_PREPARATION;
    }

    /**
     * Ce sans quoi un audit validé ne se conduit pas : vérifié à la validation, puis à chaque
     * retouche d'un audit déjà validé, qui ne doit pas pouvoir les retirer après coup.
     */
    public void exigerDatesEtResponsable() {
        if (responsableEquipe == null || dateDebutPrevue == null || dateFinPrevue == null) {
            throw new BusinessException("Un audit validé a ses dates et son responsable d'équipe : "
                    + reference + " n'en a pas.", HttpStatus.CONFLICT);
        }
    }

    public void demarrer() {
        exigerStatut("démarrer", StatutAudit.EN_PREPARATION, StatutAudit.EN_RETARD);
        statut = StatutAudit.EN_COURS;
    }

    /** Clôt l'audit. Le service a vérifié auparavant constats et circuit de signature. */
    public void cloturer() {
        exigerStatut("clôturer", StatutAudit.EN_COURS);
        statut = StatutAudit.CLOTURE;
    }

    public void annuler() {
        if (statut.estTermine()) {
            throw new BusinessException("L'audit " + reference + " est déjà " + statut
                    + " : il ne s'annule plus.", HttpStatus.CONFLICT);
        }
        statut = StatutAudit.ANNULE;
    }

    /**
     * L'audit a-t-il dépassé ses dates ?
     *
     * <p>Pas démarré alors que sa date de début est passée, ou encore en cours après sa date de
     * fin. Le jour même ne compte pas encore comme un retard.</p>
     */
    public boolean estEnRetard(LocalDate dateJour) {
        return switch (statut) {
            case PLANIFIE, EN_PREPARATION, EN_RETARD ->
                    dateDebutPrevue != null && dateJour.isAfter(dateDebutPrevue);
            case EN_COURS -> dateFinPrevue != null && dateJour.isAfter(dateFinPrevue);
            default -> false;
        };
    }

    /** Combien de jours de retard, sur la date qui compte ; nul à l'heure. */
    public Long joursDeRetard(LocalDate dateJour) {
        if (!estEnRetard(dateJour)) {
            return null;
        }
        LocalDate echeance = statut == StatutAudit.EN_COURS ? dateFinPrevue : dateDebutPrevue;
        return java.time.temporal.ChronoUnit.DAYS.between(echeance, dateJour);
    }

    /**
     * Pose le statut {@link StatutAudit#EN_RETARD} si l'audit le mérite.
     *
     * <p>Seul un audit validé mais jamais démarré y passe : voir {@link StatutAudit}.</p>
     *
     * @return vrai si le statut a changé
     */
    public boolean marquerEnRetardSiEcheance(LocalDate dateJour) {
        if (statut == StatutAudit.EN_PREPARATION && estEnRetard(dateJour)) {
            statut = StatutAudit.EN_RETARD;
            return true;
        }
        return false;
    }

    /** L'auditeur fait-il partie de l'équipe, comme responsable ou comme membre ? */
    public boolean aDansLEquipe(Auditeur auditeur) {
        if (auditeur == null) {
            return false;
        }
        return (responsableEquipe != null && responsableEquipe.getId().equals(auditeur.getId()))
                || membresEquipe.stream().anyMatch(m -> m.getId().equals(auditeur.getId()));
    }

    public void exigerModifiable() {
        if (!statut.estModifiable()) {
            throw new BusinessException("L'audit " + reference + " est " + statut
                    + " : sa planification ne se modifie plus.", HttpStatus.CONFLICT);
        }
    }

    private void exigerStatut(String geste, StatutAudit... admis) {
        for (StatutAudit s : admis) {
            if (s == statut) {
                return;
            }
        }
        throw new BusinessException("Impossible de " + geste + " l'audit " + reference
                + " : il est " + statut + ".", HttpStatus.CONFLICT);
    }
}
