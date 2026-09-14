package com.qualiapproche.workflow.service;

import com.qualiapproche.common.exception.BusinessException;
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
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.event.TransitionFranchieEvent;
import com.qualiapproche.workflow.model.FaitsDuDossier;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;

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

    /**
     * Clé du paramètre de contexte portant l'identifiant d'une action groupée.
     *
     * <p>Le contexte d'exécution est le point d'extension prévu pour ce qui appartient à
     * l'application hôte : y loger cet identifiant évite de l'ajouter en paramètre à toute la
     * chaîne du moteur, qui n'a pas à connaître la notion.</p>
     */
    protected static final String CLE_LOT = "lotId";

    /**
     * Clé du paramètre de contexte où le contrôle d'habilitation dépose, s'il y a lieu, le motif
     * précis de son refus.
     *
     * <p>Le contrôle est un prédicat : il rend vrai ou faux, et le service ne pouvait donc
     * qu'annoncer un manque d'habilitation. C'est faux dans le cas de la séparation des signatures
     * — l'auteur écarté d'une étape porte bien le rôle attendu, et se voyait répondre le contraire.
     * Le motif emprunte le contexte, prévu pour ce qui appartient à l'application hôte, plutôt que
     * d'élargir la signature du prédicat que le moteur appelle.</p>
     */
    public static final String CLE_MOTIF_REFUS = "motifRefus";

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

    /**
     * Nom de l'auteur de la décision, consigné avec elle.
     *
     * <p>Lu dans le jeton — que l'appel vienne du front ou d'un service métier, qui le propage.
     * À défaut de jeton, {@code SecurityUtils} rend « Système », ce qui vaut mieux qu'un vide :
     * une décision prise par un traitement automatique se lit alors comme telle.</p>
     */
    protected String getCurrentUserFullName() {
        return SecurityUtils.getCurrentUserFullName();
    }

    @Transactional
    public D initialiser(D pData, String pWorkflowCode) {
        if (pData == null) {
            throw new IllegalArgumentException("La donnee a initialiser est indefinie.");
        }
        pData.setWorkflowCode(pWorkflowCode);
        try {
            this.moteur.initEtatData(pData);
        } catch (WorkflowException e) {
            // Configuration de circuit inexploitable : c'est le circuit choisi qui est en cause,
            // pas le service. Signalé comme tel plutôt qu'en erreur serveur indifférenciée.
            throw new BusinessException(
                    "Ce circuit de validation n'est pas exploitable : " + e.getMessage(),
                    HttpStatus.CONFLICT);
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
        return this.transitionsPossiblesDe(this.fromDatabase(dossierId));
    }

    /**
     * Transitions franchissables pour un dossier <b>déjà chargé et dont l'état est rattaché</b>.
     *
     * <p>Existe pour le traitement par lot : passer par {@link #getTransitionsPossibles(UUID)}
     * relisait le dossier alors que l'appelant venait de le charger, doublant les allers-retours
     * pour chaque ressource d'une liste.</p>
     */
    protected Set<TransitionPersistante> transitionsPossiblesDe(D pData) {
        return this.transitionsAutorisees(this.construireContexte(pData));
    }

    @Transactional
    public D executerTransition(UUID pId, String pCodeTransition, String comments) {
        return this.executerTransition(pId, pCodeTransition, comments, null);
    }

    /**
     * Franchit une transition en rattachant le franchissement à une action groupée.
     *
     * @param lotId identifiant de l'action groupée, {@code null} pour un franchissement unitaire.
     *              Il accompagne l'événement publié : l'historique dit alors qu'une décision a été
     *              prise dans le cadre d'un traitement en masse, ce qui n'est pas indifférent à qui
     *              relit le dossier ou l'audite.
     */
    @Transactional
    public D executerTransition(UUID pId, String pCodeTransition, String comments, String lotId) {
        D pData = this.fromDatabase(pId);
        pData.setObservation(comments);
        return this.transitionDossier(pData, pCodeTransition, lotId);
    }

    protected D transitionDossier(D pData, String pCodeTransition) {
        return this.transitionDossier(pData, pCodeTransition, null);
    }

    protected D transitionDossier(D pData, String pCodeTransition, String lotId) {
        ExecutionContext<IWorkflowData> aContexte = this.construireContexte(pData);
        // Le contexte est le point d'extension prévu pour ce qui appartient à l'application hôte :
        // le porter là évite d'ajouter un paramètre à toute la chaîne du moteur.
        if (lotId != null) {
            aContexte.putParametre(CLE_LOT, lotId);
        }
        TransitionPersistante aTransition = this.resoudreTransition(pData, pCodeTransition);
        this.verifierEtatOrigine(pData, aTransition);
        this.verifierConditionMetier(pData, aTransition);
        this.verifierHabilitation(aContexte, aTransition);
        return this.updateHistorique(pData, aTransition, aContexte);
    }

    /**
     * Charge le dossier et lui rattache son état tel que le déclare son circuit.
     *
     * <p>Les trois refus possibles portent une intention métier et sont donc signalés comme tels.
     * Levés en {@code RuntimeException} nue, ils remontaient en erreur serveur : l'appelant ne
     * pouvait distinguer un dossier inexistant — cas courant, sur un identifiant erroné — d'une
     * panne du service, et une configuration de circuit incohérente passait pour un incident
     * technique.</p>
     */
    protected D fromDatabase(UUID dossierId) {
        if (dossierId == null) {
            throw new IllegalArgumentException("L'identifiant de la donnee est indefini.");
        }
        D aData = this.getRepository().findById(dossierId)
                .orElseThrow(() -> new BusinessException(
                        "Dossier de validation introuvable.", HttpStatus.NOT_FOUND));
        return this.rattacherEtat(aData);
    }

    /**
     * Rattache à un dossier déjà chargé l'état que déclare son circuit.
     *
     * <p>Séparé de {@link #fromDatabase(UUID)} pour que le traitement par lot, qui charge les
     * dossiers en une seule requête, n'ait pas à les relire un par un.</p>
     */
    protected D rattacherEtat(D pData) {
        WorkflowPersistant aWorkflow = this.workflowDe(pData);
        Etat aEtat = aWorkflow.getEtat(pData.getEtatCode());
        if (aEtat == null) {
            throw new BusinessException(
                    "L'étape courante de ce dossier (" + pData.getEtatCode() + ") n'existe plus dans son "
                            + "circuit de validation. Le circuit a été modifié : ce dossier doit être migré.",
                    HttpStatus.CONFLICT);
        }
        pData.setEtat(aEtat);
        return pData;
    }

    private WorkflowPersistant workflowDe(D pData) {
        WorkflowPersistant aWorkflow;
        try {
            aWorkflow = this.moteur.getWorkflowByCode(pData.getWorkflowCode());
        } catch (WorkflowException e) {
            throw new BusinessException(
                    "Le circuit de validation de ce dossier n'a pas pu être chargé.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (aWorkflow == null) {
            throw new BusinessException(
                    "Le circuit de validation de ce dossier n'existe plus. Il a été supprimé : "
                            + "ce dossier doit être migré vers un autre circuit.",
                    HttpStatus.CONFLICT);
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
            throw new BusinessException(
                    "Les actions possibles sur ce dossier n'ont pas pu être déterminées : " + e.getMessage(),
                    HttpStatus.CONFLICT);
        }
    }

    protected TransitionPersistante resoudreTransition(D pData, String pCodeTransition) {
        TransitionPersistante aTransition;
        try {
            aTransition = this.moteur.getTransitionByCode(pData, pCodeTransition);
        } catch (WorkflowException e) {
            throw new BusinessException(
                    "L'action demandée n'a pas pu être résolue dans ce circuit : " + e.getMessage(),
                    HttpStatus.CONFLICT);
        }
        if (aTransition == null) {
            throw new BusinessException(
                    "L'action demandée n'existe pas dans ce circuit de validation.", HttpStatus.NOT_FOUND);
        }
        return aTransition;
    }

    /**
     * Refuse le franchissement si l'utilisateur courant ne porte pas l'habilitation exigée par la
     * transition. Rendu explicite en 403 : la levée d'une {@code RuntimeException} nue renvoyait un
     * 500, indiscernable d'une panne pour l'appelant.
     */
    protected void verifierHabilitation(ExecutionContext<IWorkflowData> pContexte, TransitionPersistante pTransition) {
        if (this.transitionsAutorisees(pContexte).contains(pTransition)) {
            return;
        }
        // Le contrôle a pu dire pourquoi il refuse : c'est le cas quand ce n'est pas le rôle qui
        // manque. À défaut, le rôle attendu reste la meilleure explication disponible.
        String motif = pContexte.getParametre(CLE_MOTIF_REFUS, String.class)
                .orElseGet(() -> "Vous n'avez pas l'habilitation requise pour effectuer cette action"
                        + (pTransition.getPermission() != null
                                ? " (rôle attendu : " + pTransition.getPermission() + ")." : "."));
        throw new BusinessException(motif, HttpStatus.FORBIDDEN);
    }

    /**
     * Refuse le franchissement tant que le dossier ne remplit pas la condition exigée.
     *
     * <p>Vérifiée <b>avant</b> l'habilitation, et non après : les deux refus passent par le même
     * filtre côté moteur, et l'utilisateur se serait vu répondre qu'il n'a pas les droits alors que
     * c'est le dossier qui n'est pas prêt. Deux causes distinctes méritent deux messages.</p>
     */
    protected void verifierConditionMetier(D pData, TransitionPersistante pTransition) {
        String exigee = pTransition.getConditionRequise();
        if (exigee == null || exigee.isBlank()) {
            return;
        }
        String faits = pData instanceof WorkflowValidationInstance instance
                ? instance.getFaits() : null;
        if (!FaitsDuDossier.contient(faits, exigee)) {
            throw new BusinessException(
                    "Cette action n'est pas encore possible sur ce dossier : la condition « "
                            + exigee + " » n'est pas remplie.",
                    HttpStatus.CONFLICT);
        }
    }

    protected void verifierEtatOrigine(D pData, TransitionPersistante pTransition) {
        if (!pTransition.getEtatOrigine().equals(pData.getEtat())) {
            throw new BusinessException(
                    "Cette action ne s'applique pas à l'étape courante du dossier.", HttpStatus.CONFLICT);
        }
    }

    protected D updateHistorique(D pData, TransitionPersistante pTransition, ExecutionContext<IWorkflowData> pContexte) {
        String aEtatAvant = pData.getEtatCode();
        String aEtatAvantLibelle = pData.getEtat() != null ? pData.getEtat().getLibelle() : aEtatAvant;

        try {
            this.moteur.executerTransition(pContexte, pTransition);
        } catch (WorkflowException e) {
            throw new BusinessException(
                    "Cette action n'a pas pu être menée à son terme : " + e.getMessage(),
                    HttpStatus.CONFLICT);
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
                .validatorFullName(this.getCurrentUserFullName())
                .decisionDate(LocalDateTime.now())
                .build();

        // For Quali SIRA, ValidationHistory has a ManyToOne to WorkflowValidationInstance.
        // We will inject the instance if it's indeed D.
        if (aSauvegardee instanceof WorkflowValidationInstance) {
            history.setValidationInstance((WorkflowValidationInstance) aSauvegardee);
        }

        this.historyRepository.save(history);

        // Publish event
        // We'll create TransitionFranchieEvent in a moment.
        this.eventPublisher.publishEvent(new TransitionFranchieEvent(
                aSauvegardee.getClass().getName(),
                aSauvegardee.getId().toString(),
                aSauvegardee.getWorkflowCode(),
                pTransition.getCode(),
                aEtatAvant,
                aEtatApres,
                this.getCurrentUserId(),
                pData.getObservation(),
                pContexte.getParametre(CLE_LOT, String.class).orElse(null)
        ));

        return aSauvegardee;
    }
}
