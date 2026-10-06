package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.enumeration.StatutChecklist;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Une grille de points de contrôle, souvent calée sur un chapitre de norme.
 *
 * <p>Un modèle réutilisable d'un audit à l'autre : on la rédige en brouillon, on la publie pour
 * qu'elle serve, on l'archive quand elle ne sert plus — sans la supprimer, puisque des constats
 * passés y renvoient.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "checklist_audit")
public class ChecklistAudit extends AuditEntity {

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "chapitre_iso", length = 50)
    private String chapitreISO;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutChecklist statut;

    @Builder.Default
    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PointControle> points = new ArrayList<>();

    public void publier() {
        if (statut != StatutChecklist.BROUILLON) {
            throw new BusinessException("Seule une checklist en brouillon se publie ; « " + nom
                    + " » est " + statut + ".", HttpStatus.CONFLICT);
        }
        if (points.isEmpty()) {
            throw new BusinessException("La checklist « " + nom + " » n'a aucun point de contrôle :"
                    + " publiée, elle ne guiderait aucun audit.", HttpStatus.CONFLICT);
        }
        statut = StatutChecklist.PUBLIEE;
    }

    public void archiver() {
        statut = StatutChecklist.ARCHIVEE;
    }

    /**
     * Une copie en brouillon, points compris, pour en faire une variante.
     *
     * <p>Les points sont recopiés sans leur auditeur assigné : une affectation vaut pour un
     * usage de la grille, pas pour ses variantes.</p>
     */
    public ChecklistAudit dupliquer() {
        ChecklistAudit copie = ChecklistAudit.builder()
                .nom(nom + " (copie)")
                .chapitreISO(chapitreISO)
                .statut(StatutChecklist.BROUILLON)
                .build();
        points.forEach(p -> copie.getPoints().add(PointControle.builder()
                .checklist(copie)
                .chapitreISO(p.getChapitreISO())
                .libelle(p.getLibelle())
                .question(p.getQuestion())
                .preuveAttendue(p.getPreuveAttendue())
                .build()));
        return copie;
    }

    public void exigerModifiable() {
        if (statut != StatutChecklist.BROUILLON) {
            throw new BusinessException("La checklist « " + nom + " » est " + statut
                    + " : dupliquez-la pour en faire une variante.", HttpStatus.CONFLICT);
        }
    }
}
