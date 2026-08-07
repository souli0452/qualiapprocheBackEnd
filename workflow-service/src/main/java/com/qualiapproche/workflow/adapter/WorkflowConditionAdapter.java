package com.qualiapproche.workflow.adapter;

import com.qualiapproche.common.utils.RolesPlateforme;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.port.output.ITransitionCondition;
import com.qualiapproche.workflow.model.FaitsDuDossier;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.service.RolesUtilisateurService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Habilitation d'une transition : l'utilisateur courant doit porter le rôle exigé.
 *
 * <p>Le rôle exigé est résolu par {@code WorkflowEngineDAOAdapter} — habilitation propre à la
 * transition, à défaut rôle responsable de l'étape d'origine — et peut être désigné aussi bien par
 * son identifiant que par son nom.</p>
 *
 * <p>Les rôles de l'appelant sont demandés à user-service, seule source qui les détienne : le jeton
 * Keycloak ne porte que des rôles techniques, et l'en-tête {@code X-User-Permissions} propagé par la
 * gateway ne transporte que des permissions. Se fier au jeton revenait à ne reconnaître aucun rôle
 * métier, donc à n'autoriser aucune transition restreinte.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowConditionAdapter implements ITransitionCondition<IWorkflowData, TransitionPersistante> {

    private final RolesUtilisateurService rolesUtilisateurService;

    /**
     * Habilitation qui ne désigne pas un rôle mais la personne à qui le dossier est confié.
     *
     * <p>Écrite dans la colonne d'habilitation de la transition, elle s'y distingue d'un nom de
     * rôle par son préfixe : aucun rôle ne peut porter ce nom.</p>
     */
    private static final String HABILITATION_TITULAIRE = RolesPlateforme.HABILITATION_TITULAIRE;

    /**
     * Habilitation désignant celui qui a ouvert le dossier, distinguée d'un nom de rôle par son
     * préfixe. Voir {@link RolesPlateforme#HABILITATION_CREATEUR}.
     */
    private static final String HABILITATION_CREATEUR = RolesPlateforme.HABILITATION_CREATEUR;

    @Override
    public boolean estAutorise(ExecutionContext<IWorkflowData> pContexte, TransitionPersistante pTransition) {
        // La condition métier avant l'habilitation : une action que le dossier n'admet pas encore
        // ne doit être proposée à personne, fût-il habilité.
        if (!conditionRemplie(pContexte, pTransition)) {
            return false;
        }

        String requiredRole = pTransition.getPermission();
        if (requiredRole == null || requiredRole.trim().isEmpty()) {
            // Transition non restreinte : ouverte à tout utilisateur authentifié.
            return true;
        }

        if (HABILITATION_TITULAIRE.equalsIgnoreCase(requiredRole.trim())) {
            return estLeTitulaire(pContexte);
        }

        if (HABILITATION_CREATEUR.equalsIgnoreCase(requiredRole.trim())) {
            return estLeCreateur(pContexte);
        }

        Set<String> rolesUtilisateur = rolesUtilisateurService.rolesDeLUtilisateurCourant();
        String attendu = requiredRole.trim().toUpperCase();

        if (rolesUtilisateur.contains(attendu) || peutDeciderPartout(rolesUtilisateur)) {
            return true;
        }

        // Repli sur le jeton : couvre les circuits dont l'habilitation désigne un rôle du realm.
        return SecurityUtils.hasRole(requiredRole)
                || RolesPlateforme.DECIDE_PARTOUT.stream().anyMatch(SecurityUtils::hasRole);
    }

    /**
     * L'appelant est-il la personne à qui ce dossier a été confié ?
     *
     * <p>L'agent à qui une non-conformité est imputée traite <b>la sienne</b>. Un rôle ne sait pas
     * exprimer cela : il aurait ouvert l'étape à tous ceux qui peuvent être imputés, sur tous les
     * dossiers. La comparaison porte donc sur le titulaire inscrit à l'imputation.</p>
     *
     * <p>L'administration et la responsabilité qualité passent outre : c'est ce qui permet de
     * débloquer un dossier dont le titulaire est absent, parti, ou n'existe plus.</p>
     */
    private boolean estLeTitulaire(ExecutionContext<IWorkflowData> pContexte) {
        if (peutDeciderPartout(rolesUtilisateurService.rolesDeLUtilisateurCourant())
                || RolesPlateforme.DECIDE_PARTOUT.stream().anyMatch(SecurityUtils::hasRole)) {
            return true;
        }

        if (!(pContexte.getData() instanceof WorkflowValidationInstance instance)) {
            return false;
        }

        String titulaire = instance.getTitulaireId();
        if (titulaire == null || titulaire.isBlank()) {
            // Étape réservée au titulaire sur un dossier qui n'en a pas : personne ne peut décider,
            // et c'est préférable à l'ouvrir à tous. Le blocage se lève par l'administration.
            log.warn("Le dossier {} atteint une étape réservée à son titulaire sans qu'aucun ne soit désigné.",
                    instance.getResourceId());
            return false;
        }

        return titulaire.equals(SecurityUtils.getCurrentUserId());
    }

    /**
     * L'appelant est-il celui qui a ouvert ce dossier ?
     *
     * <p>Soumettre sa propre déclaration est un acte personnel : réservé au rôle {@code AGENT}, il
     * ouvrait le brouillon de chacun à tous les autres. Et réservé au <i>titulaire</i>, il aurait
     * cessé d'ouvrir à son auteur dès que l'imputation en désigne un autre — or le circuit ramène le
     * dossier à son auteur quand on le lui renvoie.</p>
     *
     * <p>Le créateur est inscrit à l'ouverture et jamais réécrit : la même étape lui reste donc
     * ouverte au premier comme au troisième aller-retour.</p>
     *
     * <p>L'administration passe outre, comme pour le titulaire : c'est le moyen de débloquer un
     * dossier dont l'auteur a quitté l'organisation.</p>
     */
    private boolean estLeCreateur(ExecutionContext<IWorkflowData> pContexte) {
        if (peutDeciderPartout(rolesUtilisateurService.rolesDeLUtilisateurCourant())
                || RolesPlateforme.DECIDE_PARTOUT.stream().anyMatch(SecurityUtils::hasRole)) {
            return true;
        }

        if (!(pContexte.getData() instanceof WorkflowValidationInstance instance)) {
            return false;
        }

        String createur = instance.getCreateurId();
        if (createur == null || createur.isBlank()) {
            // Circuit ouvert avant que le créateur ne soit inscrit, ou ouvert hors requête
            // utilisateur : personne ne peut décider, ce qui vaut mieux que d'ouvrir à tous. Le
            // blocage se lève par l'administration.
            log.warn("Le dossier {} atteint une étape réservée à son créateur sans qu'aucun ne soit inscrit.",
                    instance.getResourceId());
            return false;
        }

        return createur.equals(SecurityUtils.getCurrentUserId());
    }

    /**
     * Le dossier remplit-il la condition que cette transition exige ?
     *
     * <p>Le moteur ne connaît pas les plans d'action, ni l'efficacité, ni rien de ce que les
     * modules suivent : il compare le fait exigé par la transition à ceux que le module a inscrits
     * sur le dossier. La règle métier reste chez qui la détient, et le circuit peut l'exiger sans
     * la connaître.</p>
     *
     * <p>L'administration ne passe pas outre : une condition métier n'est pas une habilitation.
     * Clore une non-conformité dont les plans d'action ne sont pas soldés reste faux, quel que soit
     * celui qui le demande — c'est le dossier qui n'est pas prêt, pas l'utilisateur qui manque de
     * droits.</p>
     */
    private boolean conditionRemplie(ExecutionContext<IWorkflowData> pContexte, TransitionPersistante pTransition) {
        String exigee = pTransition.getConditionRequise();
        if (exigee == null || exigee.isBlank()) {
            return true;
        }
        if (!(pContexte.getData() instanceof WorkflowValidationInstance instance)) {
            return false;
        }
        return FaitsDuDossier.contient(instance.getFaits(), exigee);
    }

    /**
     * L'appelant peut-il décider à n'importe quelle étape ?
     *
     * <p>L'administration seule, pour débloquer un dossier que son étape courante n'attribue à
     * personne de disponible. Le responsable qualité en a été retiré : il voit tous les dossiers,
     * mais n'agit qu'aux étapes qui lui sont confiées — sans quoi l'habilitation portée par les
     * étapes n'aurait plus de sens à son égard.</p>
     */
    private boolean peutDeciderPartout(Set<String> rolesUtilisateur) {
        return rolesUtilisateur.stream().anyMatch(RolesPlateforme.DECIDE_PARTOUT::contains);
    }
}
