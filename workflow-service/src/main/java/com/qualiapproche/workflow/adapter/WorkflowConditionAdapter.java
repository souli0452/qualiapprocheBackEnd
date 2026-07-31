package com.qualiapproche.workflow.adapter;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.port.output.ITransitionCondition;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Habilitation d'une transition : l'utilisateur courant doit porter le rôle exigé.
 *
 * <p>Le rôle exigé est résolu par {@code WorkflowEngineDAOAdapter} (habilitation propre à la
 * transition, à défaut rôle responsable de l'étape d'origine).</p>
 */
@Component
@Slf4j
public class WorkflowConditionAdapter implements ITransitionCondition<IWorkflowData, TransitionPersistante> {

    /** Rôle d'administration autorisé à débloquer un dossier quelle que soit l'étape. */
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    /** Un identifiant technique ne peut pas correspondre à un rôle porté par le jeton. */
    private static final java.util.regex.Pattern IDENTIFIANT_TECHNIQUE = java.util.regex.Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Override
    public boolean estAutorise(ExecutionContext<IWorkflowData> pContexte, TransitionPersistante pTransition) {
        String requiredRole = pTransition.getPermission();
        if (requiredRole == null || requiredRole.trim().isEmpty()) {
            // Transition non restreinte : ouverte à tout utilisateur authentifié.
            return true;
        }

        if (IDENTIFIANT_TECHNIQUE.matcher(requiredRole.trim()).matches()) {
            // Le jeton porte des noms de rôle, jamais leur identifiant en base : une habilitation
            // renseignée sous forme d'identifiant ne peut correspondre à personne, et l'étape
            // n'offrirait plus aucune action sans que la cause soit visible.
            log.error("Habilitation inexploitable sur la transition {} : « {} » est un identifiant "
                            + "technique, or le contrôle porte sur le nom du rôle. Corrigez le rôle "
                            + "responsable de l'étape dans le catalogue.",
                    pTransition.getCode(), requiredRole);
            return false;
        }

        return SecurityUtils.hasRole(requiredRole) || SecurityUtils.hasRole(ROLE_SUPER_ADMIN);
    }
}
