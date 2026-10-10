package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.enumeration.OrigineDocument;
import com.qualiapproche.evaluation.enumeration.StatutPlanAudit;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Le plan d'un audit : son déroulé, communiqué à l'audité avant la visite.
 *
 * <p>Un plan par audit. Il se rédige ici, activité par activité, ou se reçoit tout fait et se
 * charge ({@code origine} le dit). Une fois partagé avec l'audité, il ne se modifie plus : ce qui
 * a été annoncé doit rester ce qui a été annoncé.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "plan_audit")
public class PlanAudit extends AuditEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false, unique = true)
    private Audit audit;

    @Column(name = "reference", length = 60)
    private String reference;

    @Column(name = "objectifs", columnDefinition = "TEXT")
    private String objectifs;

    @Column(name = "portee", columnDefinition = "TEXT")
    private String portee;

    @Column(name = "criteres", columnDefinition = "TEXT")
    private String criteres;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "plan_audit_methode", joinColumns = @JoinColumn(name = "plan_audit_id"))
    @Column(name = "valeur", nullable = false)
    private List<String> methodesAudit = new ArrayList<>();

    @Column(name = "lieu")
    private String lieu;

    @Column(name = "langue", length = 50)
    private String langue;

    @Column(name = "moyens_necessaires", columnDefinition = "TEXT")
    private String moyensNecessaires;

    @Enumerated(EnumType.STRING)
    @Column(name = "origine", nullable = false, length = 20)
    private OrigineDocument origine;

    /** Référence du plan reçu sur le serveur de fichiers, quand il a été chargé. */
    @Column(name = "fichier_recu", columnDefinition = "TEXT")
    private String fichierRecu;

    /** Nom du fichier tel que l'utilisateur l'a chargé, extension comprise. */
    @Column(name = "nom_fichier_recu", length = 255)
    private String nomFichierRecu;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutPlanAudit statut;

    @Column(name = "date_partage")
    private LocalDateTime datePartage;

    @Builder.Default
    @OrderBy("horaireDebut ASC")
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivitePlan> activites = new ArrayList<>();

    public void valider() {
        if (statut != StatutPlanAudit.BROUILLON) {
            throw new BusinessException("Le plan est déjà " + statut + ".", HttpStatus.CONFLICT);
        }
        if (activites.isEmpty() && fichierRecu == null) {
            throw new BusinessException("Un plan sans activité ni fichier chargé ne se valide pas :"
                    + " il n'annoncerait rien à l'audité.", HttpStatus.CONFLICT);
        }
        statut = StatutPlanAudit.VALIDE;
    }

    public void partagerAvecAudite() {
        if (statut != StatutPlanAudit.VALIDE) {
            throw new BusinessException("Seul un plan validé se partage avec l'audité ; celui-ci est "
                    + statut + ".", HttpStatus.CONFLICT);
        }
        statut = StatutPlanAudit.PARTAGE;
        datePartage = LocalDateTime.now();
    }

    public void exigerModifiable() {
        if (statut != StatutPlanAudit.BROUILLON) {
            throw new BusinessException("Le plan est " + statut + " : il ne se modifie plus.",
                    HttpStatus.CONFLICT);
        }
    }
}
