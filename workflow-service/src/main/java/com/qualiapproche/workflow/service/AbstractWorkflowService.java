package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.model.ValidationHistory;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service abstrait fournissant le socle de base pour la gestion des workflows.
 * 
 * <p>Ce service implémente les opérations fondamentales du cycle de vie d'une donnée
 * soumise à un workflow (initialisation, calcul des transitions possibles, exécution d'une transition, 
 * vérification des habilitations et historisation). Les services métiers spécifiques doivent 
 * hériter de cette classe et implémenter les méthodes abstraites pour lier le moteur 
 * à leurs propres dépôts de données.</p>
 *
 * @param <D> Le type de l'entité gérée, devant implémenter {@link IWorkflowData}
 */
public abstract class AbstractWorkflowService<D extends IWorkflowData> {

    protected final IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> moteur;
    protected final ValidationHistoryRepository historyRepository;
    protected final ApplicationEventPublisher eventPublisher;

    protected AbstractWorkflowService(
            IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> moteur,
            ValidationHistoryRepository historyRepository,
            ApplicationEventPublisher eventPublisher) {
        this.moteur = moteur;
        this.historyRepository = historyRepository;
        this.eventPublisher = eventPublisher;
    }

    protected abstract JpaRepository<D, UUID> getRepository();

    protected abstract String getWorkflowCode();

    protected abstract String getCurrentUserId();

    @Transactional
    public D initialiser(D pData, String pWorkflowCode) {
        if (pData == null) {
            throw new IllegalArgumentException("La donnee a initialiser est indefinie.");
        }
        pData.setWorkflowCode(pWorkflowCode);
        try {
            this.moteur.initEtatData(pData);
        } catch (WorkflowException e) {
            throw new RuntimeException("Erreur lors de l'initialisation du workflow", e);
        }
        this.synchroniserEtat(pData);
        return this.getRepository().save(pData);
    }

    @Transactional
    public D initialiser(D pData) {
        return this.initialiser(pData, this.getWorkflowCode());
    }

    @Transactional(readOnly = true)
    public Set<TransitionPersistante> getTransitionsPossibles(UUID dossierId) {
        D aData = this.fromDatabase(dossierId);
        return this.transitionsAutorisees(this.construireContexte(aData));
    }

    @Transactional
    public D executerTransition(UUID pId, String pCodeTransition, String comments) {
        D pData = this.fromDatabase(pId);
        pData.setObservation(comments);
        return this.transitionDossier(pData, pCodeTransition);
    }

    protected D transitionDossier(D pData, String pCodeTransition) {
        ExecutionContext<IWorkflowData> aContexte = this.construireContexte(pData);
        TransitionPersistante aTransition = this.resoudreTransition(pData, pCodeTransition);
        this.verifierEtatOrigine(pData, aTransition);
        this.verifierHabilitation(aContexte, aTransition);
        return this.updateHistorique(pData, aTransition, aContexte);
    }

    protected D fromDatabase(UUID dossierId) {
        if (dossierId == null) {
            throw new IllegalArgumentException("L'identifiant de la donnee est indefini.");
        }
        D aData = this.getRepository().findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable: " + dossierId));

        WorkflowPersistant aWorkflow = this.workflowDe(aData, dossierId);
        Etat aEtat = aWorkflow.getEtat(aData.getEtatCode());
        if (aEtat == null) {
            throw new RuntimeException("L'etat " + aData.getEtatCode() + " n'est pas declare par le workflow.");
        }
        aData.setEtat(aEtat);
        return aData;
    }

    private WorkflowPersistant workflowDe(D pData, UUID pId) {
        WorkflowPersistant aWorkflow;
        try {
            aWorkflow = this.moteur.getWorkflowByCode(pData.getWorkflowCode());
        } catch (WorkflowException e) {
            throw new RuntimeException("Erreur lors de la recupération du workflow", e);
        }
        if (aWorkflow == null) {
            throw new RuntimeException("Le workflow " + pData.getWorkflowCode() + " est inconnu.");
        }
        return aWorkflow;
    }

    protected void synchroniserEtat(D pData) {
        Etat aEtat = pData.getEtat();
        if (aEtat == null) {
            pData.appliquerEtat(null);
            return;
        }
        
        // We set the state and code back to the entity.
        pData.appliquerEtat(aEtat);
    }

    protected ExecutionContext<IWorkflowData> construireContexte(D pData) {
        ExecutionContext<IWorkflowData> aContexte = new ExecutionContext<>(pData);
        aContexte.putParametre("utilisateur", this.getCurrentUserId());
        // Populate additional roles/permissions if required by WorkflowConditionAdapter
        if (pData.getObservation() != null) {
            aContexte.putParametre("commentaire", pData.getObservation());
        }
        return aContexte;
    }

    private Set<TransitionPersistante> transitionsAutorisees(ExecutionContext<IWorkflowData> pContexte) {
        try {
            // Because IWorkflowEngine returns SequencedSet/Set we cast properly
            return new LinkedHashSet<>(this.moteur.getTransitionsPossibles(pContexte));
        } catch (WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    protected TransitionPersistante resoudreTransition(D pData, String pCodeTransition) {
        TransitionPersistante aTransition;
        try {
            aTransition = this.moteur.getTransitionByCode(pData, pCodeTransition);
        } catch (WorkflowException e) {
            throw new RuntimeException("Erreur de resolution de transition", e);
        }
        if (aTransition == null) {
            throw new RuntimeException("La transition " + pCodeTransition + " est inconnue.");
        }
        return aTransition;
    }

    protected void verifierHabilitation(ExecutionContext<IWorkflowData> pContexte, TransitionPersistante pTransition) {
        if (!this.transitionsAutorisees(pContexte).contains(pTransition)) {
            throw new RuntimeException("Transition interdite pour l'utilisateur courant.");
        }
    }

    protected void verifierEtatOrigine(D pData, TransitionPersistante pTransition) {
        if (!pTransition.getEtatOrigine().equals(pData.getEtat())) {
            throw new RuntimeException("Etat d'origine invalide pour cette transition.");
        }
    }

    protected D updateHistorique(D pData, TransitionPersistante pTransition, ExecutionContext<IWorkflowData> pContexte) {
        String aEtatAvant = pData.getEtatCode();
        String aEtatAvantLibelle = pData.getEtat() != null ? pData.getEtat().getLibelle() : aEtatAvant;

        try {
            this.moteur.executerTransition(pContexte, pTransition);
        } catch (WorkflowException e) {
            throw new RuntimeException("Erreur d'execution de la transition", e);
        }
        
        this.synchroniserEtat(pData);
        String aEtatApres = pData.getEtatCode();
        String aEtatApresLibelle = pData.getEtat() != null ? pData.getEtat().getLibelle() : aEtatApres;

        D aSauvegardee = this.getRepository().save(pData);

        // Record history
        ValidationHistory history = ValidationHistory.builder()
                // If it's a ValidationInstance, we set it, otherwise we could make it generic.
                // Assuming history is decoupled, we should only store reference if applicable.
                .stepCode(aEtatAvant)
                .stepName(aEtatAvantLibelle)
                .decision(pTransition.getLibelle() != null ? pTransition.getLibelle() : "Action exécutée")
                .comments(pData.getObservation())
                .validatorUserId(this.getCurrentUserId())
                .decisionDate(LocalDateTime.now())
                .build();
                
        // For Quali SIRA, ValidationHistory has a ManyToOne to WorkflowValidationInstance.
        // We will inject the instance if it's indeed D.
        if (aSauvegardee instanceof com.qualiapproche.workflow.model.WorkflowValidationInstance) {
            history.setValidationInstance((com.qualiapproche.workflow.model.WorkflowValidationInstance) aSauvegardee);
        }

        this.historyRepository.save(history);

        // Publish event
        // We'll create TransitionFranchieEvent in a moment.
        this.eventPublisher.publishEvent(new com.qualiapproche.workflow.event.TransitionFranchieEvent(
                aSauvegardee.getClass().getName(),
                aSauvegardee.getId().toString(),
                aSauvegardee.getWorkflowCode(),
                pTransition.getCode(),
                aEtatAvant,
                aEtatApres,
                this.getCurrentUserId(),
                pData.getObservation()
        ));

        return aSauvegardee;
    }
}
