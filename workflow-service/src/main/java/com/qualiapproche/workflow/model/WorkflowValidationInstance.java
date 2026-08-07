package com.qualiapproche.workflow.model;

import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_validation_instance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowValidationInstance implements IWorkflowData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String resourceId; // UUID or ID as String

    @Column(nullable = false)
    private String resourceType; // e.g. "DOCUMENT", "NON_CONFORMITE"

    @Column(name = "workflow_code", nullable = false)
    private String workflowCode;

    @Column(name = "etat_code", nullable = false)
    private String etatCode;

    @Column(name = "observation", length = 2000)
    private String observation;

    /**
     * Personne à qui le dossier a été nominativement confié.
     *
     * <p>Certaines étapes ne se décident pas par appartenance à un rôle mais par désignation :
     * l'agent à qui une non-conformité est imputée traite <b>la sienne</b>, pas celle d'un
     * collègue. Habilitée par un rôle — fût-il nommé « agent imputé » — l'étape se serait ouverte
     * à tous ceux qui peuvent être imputés, sur tous les dossiers.</p>
     *
     * <p>Renseignée par l'étape qui désigne, via {@code WorkflowStep.champTitulaire}, et exigée par
     * les transitions dont l'habilitation vaut {@code @TITULAIRE}.</p>
     */
    @Column(name = "titulaire_id")
    private String titulaireId;

    /**
     * Personne qui a ouvert le dossier : le déclarant, le déposant, le demandeur.
     *
     * <p>Inscrite à l'ouverture du circuit et <b>jamais réécrite</b> — c'est ce qui la distingue du
     * titulaire, que l'imputation déplace. Les deux notions tenaient dans un seul champ, si bien
     * qu'une étape que le dossier retraverse — « Renvoyer au déclarant » ramène à la soumission —
     * n'ouvrait plus à personne dès qu'un titulaire avait été désigné ailleurs.</p>
     *
     * <p>Exigée par les transitions dont l'habilitation vaut {@code @CREATEUR}. Aucune étape n'est
     * nommée dans le code : c'est la configuration du circuit qui décide où cette habilitation
     * s'applique.</p>
     */
    @Column(name = "createur_id", updatable = false)
    private String createurId;

    /**
     * Faits établis sur ce dossier, séparés par des virgules.
     *
     * <p>Le module métier les déclare — « les plans d'action sont tous soldés », « l'efficacité est
     * mesurée » — et les transitions les exigent. Le moteur n'a ainsi à connaître ni plan d'action
     * ni efficacité : il compare des chaînes, et la règle métier reste chez qui la détient.</p>
     *
     * <p>Une colonne de texte plutôt qu'une table : un dossier en porte quelques-uns, jamais
     * une collection qu'on aurait à interroger pour elle-même.</p>
     */
    @Column(name = "faits", length = 2000)
    private String faits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatus status;

    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @Transient
    private Etat etatCourant;

    @Override
    public Etat getEtat() {
        return this.etatCourant;
    }

    @Override
    public void setEtat(Etat pEtat) {
        this.etatCourant = pEtat;
    }

    @Override
    public void appliquerEtat(Etat pEtat) {
        this.setEtat(pEtat);
        this.setEtatCode(pEtat != null ? pEtat.getCode() : null);
    }
}
