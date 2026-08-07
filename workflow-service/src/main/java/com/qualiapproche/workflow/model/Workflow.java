package com.qualiapproche.workflow.model;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "workflow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Workflow extends AuditEntity {

    @Column(nullable = false)
    private String nom; // ex: Validation Standard, Validation Complète

    private String description;

    @Column(nullable = false)
    private String resourceType; // ex: "DOCUMENT", "NON_CONFORMITE"

    /**
     * Un seul workflow doit être actif par resourceType : c'est celui que les services
     * métier utilisent pour initier une instance. Sans ce marqueur explicite, le premier
     * workflow renvoyé par la base (ordre non garanti) était choisi arbitrairement.
     * {@code columnDefinition} force les lignes existantes à {@code true} lors de la migration.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean actif = true;

    /**
     * Entité à laquelle ce circuit est <b>réservé</b> au sein de sa famille, ou {@code null} s'il
     * vaut pour toute la famille.
     *
     * <p>Un type de document désigne ainsi son propre circuit : le circuit porte la famille
     * {@code DOCUMENT} et la cible {@code <identifiant du type>}. Les dossiers dont le type ne
     * correspond à aucune cible prennent le circuit <b>sans cible</b> — le circuit par défaut de la
     * famille, celui livré au premier démarrage.</p>
     *
     * <p>La famille n'est pas subdivisée pour autant, et c'est délibéré : {@code resourceType} est
     * l'aiguillage de retour vers le module métier — la remise des notifications
     * ({@code WorkflowNotificationService}) et la liste « à traiter » ne connaissent que DOCUMENT,
     * DEMANDE_DOCUMENT, NON_CONFORMITE et PLAN_ACTION. Une famille {@code TYPE_DOCUMENT} aurait
     * rendu ces dossiers muets et invisibles. La réservation est donc un second axe, orthogonal à
     * la famille.</p>
     *
     * <p>L'unicité du couple (famille, cible) fait la cohérence de l'ensemble : deux circuits
     * réservés au même type, ou deux circuits par défaut d'une même famille, rendraient le choix
     * arbitraire. Elle est tenue par le service — voir
     * {@code WorkflowService#verifierUniciteDeLaCible} — la contrainte de base venant dans un
     * second temps, une fois les configurations existantes démêlées : ajoutée d'emblée, elle aurait
     * échoué en silence sur les installations qui portent déjà deux circuits sans cible.</p>
     *
     * <p>Volontairement une chaîne opaque, et non une clé étrangère : l'identifiant appartient à un
     * autre service, comme les faits exigés par une transition. Le moteur ne cherche pas à savoir
     * ce qu'elle désigne.</p>
     *
     * <p>À ne pas confondre avec {@code WorkflowValidationInstance.resourceId}, qui désigne un
     * <b>dossier</b> — un document précis. Celui-ci désigne une <b>catégorie</b> de dossiers.</p>
     */
    @Column(name = "cible_id", length = 100)
    private String cibleId;

    /** Ce circuit vaut-il pour toute sa famille, faute de cible ? */
    public boolean estLeCircuitParDefaut() {
        return cibleId == null || cibleId.isBlank();
    }

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    @JsonIgnoreProperties("workflow")
    private List<WorkflowStep> steps = new ArrayList<>();

    public void addStep(WorkflowStep step) {
        steps.add(step);
        step.setWorkflow(this);
    }
}
