package com.qualiapproche.workflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "workflow_transition",
        // Une étape offrait au plus deux actions, l'unicité portant sur sa décision : impossible d'y
        // ajouter « Demander un complément » à côté de « Valider », l'une et l'autre approuvant.
        // C'est le code de l'action qui l'identifie désormais au sein de son étape ; la décision
        // dit seulement de quelle nature elle est.
        uniqueConstraints = @UniqueConstraint(name = "uk_workflow_transition_from_code",
                columnNames = {"from_step_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_step_id", nullable = false)
    @JsonIgnore
    private WorkflowStep fromStep;

    /**
     * Ce que l'action est, au sein de son étape : {@code APPROUVE}, {@code REJETE},
     * {@code DEMANDER_COMPLEMENT}, {@code TRANSFERER}…
     *
     * <p>Une étape n'offrait que deux issues, parce que l'action se confondait avec sa décision. Or
     * une même étape peut proposer plusieurs suites de même nature — valider, ou valider en
     * demandant un complément — qui ne mènent pas au même endroit et n'attendent pas la même
     * saisie. Le code les distingue ; la {@link #decision} dit seulement si l'action fait avancer
     * le dossier ou le renvoie en arrière.</p>
     *
     * <p>C'est une clé stable, unique dans l'étape, sur laquelle s'appuient l'appariement lors
     * d'une modification du circuit et le rattachement d'un champ à une action précise. Les
     * transitions antérieures reçoivent le nom de leur décision, qu'elles portaient de fait.</p>
     */
    @Column(name = "code", length = 60)
    private String code;

    /**
     * Nature de l'action : elle fait avancer le dossier ({@code APPROUVE}), le renvoie en arrière
     * ({@code REJETE}) ou le met en clôture ({@code CLOTURE}).
     *
     * <p>Ce n'est plus l'identité de l'action — voir {@link #code} — mais elle reste ce qui donne
     * son sens à un franchissement : la couleur du bouton par défaut, l'issue publiée aux modules
     * métier quand le circuit se termine, et la portée des champs demandés à la décision.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepDecision decision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_step_id")
    @JsonIgnoreProperties({"transitions", "fields", "workflow"})
    private WorkflowStep toStep;

    private String requiredRole;

    /** Libellé du bouton d'action. À défaut, le nom de la décision est repris. */
    private String label;

    /**
     * Icône du bouton d'action, exprimée en classe PrimeIcons ({@code "pi pi-check"}).
     *
     * <p>Stockée telle qu'elle est saisie : la liste des icônes appartient au thème du client, et
     * la figer côté serveur obligerait à livrer le back à chaque nouvelle icône.</p>
     */
    @Column(name = "icon")
    private String icon;

    /** Couleur du bouton d'action. À défaut, celle que porte la décision. */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private SeveriteAction severity;

    /**
     * Fait qui doit être établi sur le dossier pour que la transition soit franchissable.
     *
     * <p>Une décision ne dépend pas que de qui la prend : clore une non-conformité n'a de sens que
     * si ses plans d'action sont soldés. Le moteur ne connaît pas les plans d'action — il ne connaît
     * que des <b>faits</b>, chaînes que le module métier inscrit sur l'instance quand la condition
     * devient vraie. L'un déclare, l'autre exige, et aucun n'a besoin de connaître l'autre.</p>
     *
     * <p>Nul : la transition ne dépend d'aucune condition métier.</p>
     */
    @Column(name = "condition_requise")
    private String conditionRequise;

    /**
     * Ce que la condition veut dire, en clair.
     *
     * <p>Le nom du fait est technique — {@code PLANS_ACTION_SOLDES} — et l'écran ne peut pas le
     * traduire sans se doter d'une table de correspondance en parallèle, qui mentirait dès qu'un
     * circuit déclarerait un fait nouveau. C'est donc l'auteur du circuit qui écrit la phrase, là
     * où il pose la condition.</p>
     */
    @Column(length = 500)
    private String conditionLibelle;

    /**
     * La transition clôt le circuit au lieu de mener à une autre étape.
     *
     * <p>Marqueur explicite, et non déduit de l'absence de destination : une transition sans
     * destination pouvait aussi bien signifier « cette décision termine le dossier » que
     * « cette décision n'a pas lieu d'être ici ». Le moteur proposait donc comme action des
     * transitions que personne n'avait voulues.</p>
     */
    @Column(name = "terminal", nullable = false)
    @Builder.Default
    private boolean terminal = false;

    /**
     * Donne son code à une action qui n'en porte pas : celui de sa décision, qu'elle avait de fait.
     *
     * <p>Filet de dernier recours, et non la règle : c'est à l'enregistrement du circuit qu'un code
     * unique est attribué, seul moment où l'on voit les autres actions de l'étape. Laisser un code
     * nul serait pire que faux — l'unicité en base ne retient pas deux valeurs nulles, et deux
     * actions indistinctes se glisseraient sur la même étape.</p>
     */
    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    void codeParDefaut() {
        code = codeEffectif();
    }

    /**
     * Code sous lequel cette action se désigne, y compris avant tout enregistrement.
     *
     * <p>Les circuits construits en mémoire — celui que l'initialiseur livre, celui auquel le
     * rattrapage compare les circuits en base — n'ont pas encore traversé la persistance : lire
     * {@code code} directement y rendrait {@code null}, et toute comparaison échouerait en
     * silence.</p>
     */
    public String codeEffectif() {
        if (code != null && !code.isBlank()) {
            return code;
        }
        return decision != null ? decision.name() : null;
    }
}
