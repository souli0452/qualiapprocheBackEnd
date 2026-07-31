package com.qualiapproche.workflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Événement publié à chaque franchissement de transition.
 *
 * <p>Point d'accroche des effets de bord non transactionnels — notification, appel à un
 * service externe, génération de document. Les consommateurs doivent s'abonner avec
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} : un effet de bord déclenché
 * avant le commit ne serait pas annulable, et une action groupée échouant sur son dernier
 * dossier aurait déjà notifié tous les précédents.</p>
 *
 * @param typeEntite      nom qualifié de la classe de la donnée concernée
 * @param entiteId        identifiant de la donnée concernée
 * @param workflowCode    code du workflow pilotant la donnée
 * @param transitionCode  code de la transition franchie
 * @param etatAvantCode   code de l'état quitté
 * @param etatApresCode   code de l'état atteint
 * @param auteur          identifiant de l'utilisateur ayant franchi la transition
 * @param commentaire     commentaire saisi lors du franchissement
 * @param lotId           identifiant de l'action groupée, null pour un franchissement unitaire
 */
@Getter
public class TransitionFranchieEvent extends ApplicationEvent {

    private final String entityClass;
    private final String entityId;
    private final String workflowCode;
    private final String transitionCode;
    private final String etatAvant;
    private final String etatApres;
    private final String auteurId;
    private final String commentaire;
    private final String lotId;

    public TransitionFranchieEvent(String entityClass, String entityId, String workflowCode, String transitionCode,
                                   String etatAvant, String etatApres, String auteurId, String commentaire, String lotId) {
        super(entityId);
        this.entityClass = entityClass;
        this.entityId = entityId;
        this.workflowCode = workflowCode;
        this.transitionCode = transitionCode;
        this.etatAvant = etatAvant;
        this.etatApres = etatApres;
        this.auteurId = auteurId;
        this.commentaire = commentaire;
        this.lotId = lotId;
    }
    
    public TransitionFranchieEvent(String entityClass, String entityId, String workflowCode, String transitionCode,
                                   String etatAvant, String etatApres, String auteurId, String commentaire) {
        this(entityClass, entityId, workflowCode, transitionCode, etatAvant, etatApres, auteurId, commentaire, null);
    }
}
