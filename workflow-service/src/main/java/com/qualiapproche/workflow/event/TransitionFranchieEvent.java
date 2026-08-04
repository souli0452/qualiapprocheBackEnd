package com.qualiapproche.workflow.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

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

    /**
     * Notification enregistrée avant le commit, à remettre une fois celui-ci acquis.
     *
     * <p>Les deux phases d'écoute reçoivent la <b>même</b> instance d'événement : celle-ci est
     * donc le support naturel pour passer l'identifiant de l'une à l'autre. Il transitait
     * jusqu'ici par un {@code ThreadLocal}, avec deux défauts — un commit en échec n'atteignant
     * jamais la phase suivante, la valeur restait accrochée à un fil de pool et polluait la
     * requête d'après ; et deux transitions dans une même transaction se seraient écrasées
     * mutuellement, seule la dernière étant remise sans attendre l'ordonnanceur.</p>
     */
    @Setter
    private UUID notificationId;

    /**
     * Ressource métier pilotée par l'instance, renseignée en même temps que la notification.
     *
     * <p>{@code entityId} désigne l'instance de validation, pas le document ou la non-conformité
     * dont il est question : un courriel qui la citerait renverrait le lecteur vers un
     * identifiant qui ne lui parle pas. L'écouteur charge déjà l'instance avant le commit ; il
     * dépose ici ce qu'il en sait, plutôt que de la relire ensuite.</p>
     */
    @Setter
    private String resourceId;

    @Setter
    private String resourceType;

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
