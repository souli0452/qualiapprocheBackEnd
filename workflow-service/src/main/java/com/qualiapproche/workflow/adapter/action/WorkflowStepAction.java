package com.qualiapproche.workflow.adapter.action;

import com.qualiapproche.workflow.core.action.DefaultTransitionAction;
import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.model.ActionExecutionContext;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Action de franchissement d'une étape : positionne l'état de destination et clôt l'instance
 * lorsque cet état est terminal.
 *
 * <p>Cette action ne produit plus d'effet externe. Les webhooks et e-mails qu'elle émettait
 * partaient <b>à l'intérieur</b> de la transaction, avant commit : un rollback ultérieur laissait
 * le service métier avec une décision jamais enregistrée, et l'e-mail d'étape faisait doublon
 * avec celui de {@code WorkflowEventListener}. Ces effets sont désormais publiés une seule fois,
 * après commit, par {@link WorkflowEventListener}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowStepAction extends DefaultTransitionAction<WorkflowValidationInstance, Transition<WorkflowValidationInstance>> {

    private static final String PREFIXE_ETAT_TERMINAL = "TERMINATED_";

    @Override
    protected void update(ActionExecutionContext<WorkflowValidationInstance, Transition<WorkflowValidationInstance>> pContexte)
            throws WorkflowException {
        super.update(pContexte); // positionne l'état de destination

        WorkflowValidationInstance instance = pContexte.getData();
        Etat destination = pContexte.getTransition().getEtatDestination();

        if (destination.getCode().startsWith(PREFIXE_ETAT_TERMINAL)) {
            instance.setStatus(ValidationStatus.TERMINE);
            instance.setCompletedAt(LocalDateTime.now());
        } else {
            instance.setStatus(ValidationStatus.EN_COURS);
        }
    }
}
