package com.qualiapproche.workflow.adapter;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.port.output.ITransitionCondition;
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

    /** Rôle d'administration autorisé à débloquer un dossier quelle que soit l'étape. */
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private final RolesUtilisateurService rolesUtilisateurService;

    @Override
    public boolean estAutorise(ExecutionContext<IWorkflowData> pContexte, TransitionPersistante pTransition) {
        String requiredRole = pTransition.getPermission();
        if (requiredRole == null || requiredRole.trim().isEmpty()) {
            // Transition non restreinte : ouverte à tout utilisateur authentifié.
            return true;
        }

        Set<String> rolesUtilisateur = rolesUtilisateurService.rolesDeLUtilisateurCourant();
        String attendu = requiredRole.trim().toUpperCase();

        if (rolesUtilisateur.contains(attendu) || rolesUtilisateur.contains(ROLE_SUPER_ADMIN)) {
            return true;
        }

        // Repli sur le jeton : couvre les circuits dont l'habilitation désigne un rôle du realm.
        return SecurityUtils.hasRole(requiredRole) || SecurityUtils.hasRole(ROLE_SUPER_ADMIN);
    }
}
