package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import lombok.Builder;
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
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * L'évaluation d'une structure au regard du référentiel RQAPBF, menée lors d'un audit.
 *
 * <p>Le score global est la moyenne des critères notés, hors critères non applicables : voir
 * {@link com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF}.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "evaluation_rqapbf")
public class EvaluationRQAPBF extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;

    @Column(name = "score_global")
    private Double scoreGlobal;

    @Column(name = "niveau_conformite", length = 50)
    private String niveauConformite;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutEvaluation statut;

    @Builder.Default
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotationCritere> notations = new ArrayList<>();

    /**
     * Recalcule le score global et le niveau de conformité qu'il désigne.
     *
     * @return le score, ou {@code null} si aucun critère applicable n'est noté
     */
    public Double calculerScoreGlobal() {
        scoreGlobal = moyenne(notations);
        niveauConformite = niveauDe(scoreGlobal);
        return scoreGlobal;
    }

    /**
     * Le score par domaine — l'ancêtre racine de chaque critère noté —, dans l'ordre du référentiel.
     *
     * <p>Regroupé par identité, et non par {@code equals} : celui qu'AuditEntity tient de Lombok
     * compare les seuls champs communs, si bien que deux domaines pas encore enregistrés se
     * confondaient en un.</p>
     */
    public Map<String, Double> genererResultatsParDomaine() {
        return genererResultatsParDomaine(false);
    }

    /**
     * Comme {@link #genererResultatsParDomaine()}, limité aux critères publiés si demandé : ce sont
     * les résultats officiels, ceux du rapport. Les brouillons n'y entrent qu'en prévisualisation.
     */
    public Map<String, Double> genererResultatsParDomaine(boolean publiesSeulement) {
        Map<NoeudReferentiel, List<NotationCritere>> parDomaine = retenues(publiesSeulement).stream()
                .collect(Collectors.groupingBy(n -> n.getNoeud().racine(), IdentityHashMap::new, Collectors.toList()));
        Map<String, Double> resultats = new LinkedHashMap<>();
        parDomaine.entrySet().stream()
                .sorted((a, b) -> Integer.compare(a.getKey().getOrdre(), b.getKey().getOrdre()))
                .forEach(e -> resultats.put(e.getKey().getCode() + " — " + e.getKey().getLibelle(),
                        moyenne(e.getValue())));
        return resultats;
    }

    /** Le score officiel : la moyenne des seuls critères publiés. */
    public Double scoreOfficiel() {
        return moyenne(retenues(true));
    }

    /**
     * Le score de chaque nœud intermédiaire — domaine, section, exigence —, par code : la moyenne
     * des critères notés qui en descendent. C'est ce que les écrans de sélection affichent à
     * chaque niveau de l'arbre.
     */
    public Map<String, Double> scoresParNoeud(boolean publiesSeulement) {
        Map<NoeudReferentiel, List<NotationCritere>> parNoeud = new IdentityHashMap<>();
        for (NotationCritere n : retenues(publiesSeulement)) {
            for (NoeudReferentiel p = n.getNoeud().getParent(); p != null; p = p.getParent()) {
                parNoeud.computeIfAbsent(p, k -> new ArrayList<>()).add(n);
            }
        }
        Map<String, Double> scores = new LinkedHashMap<>();
        parNoeud.forEach((noeud, liste) -> scores.put(noeud.getCode(), moyenne(liste)));
        return scores;
    }

    private List<NotationCritere> retenues(boolean publiesSeulement) {
        return publiesSeulement ? notations.stream().filter(NotationCritere::estPublie).toList() : notations;
    }

    static Double moyenne(List<NotationCritere> notees) {
        List<Double> scores = notees.stream().map(NotationCritere::calculerScore)
                .filter(Objects::nonNull).toList();
        if (scores.isEmpty()) {
            return null;
        }
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    /**
     * Le niveau qu'un score désigne. Seuils fixés ici faute de paramétrage dans le modèle :
     * ils se déplaceront vers un référentiel si une organisation en demande d'autres.
     */
    static String niveauDe(Double score) {
        if (score == null) {
            return null;
        }
        if (score >= 90) {
            return "CONFORME";
        }
        if (score >= 70) {
            return "ACCEPTABLE";
        }
        if (score >= 50) {
            return "A_AMELIORER";
        }
        return "NON_CONFORME";
    }
}
