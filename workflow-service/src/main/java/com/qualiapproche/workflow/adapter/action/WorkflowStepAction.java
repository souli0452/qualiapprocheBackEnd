package com.qualiapproche.workflow.adapter.action;

import com.qualiapproche.workflow.core.action.DefaultTransitionAction;
import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.model.ActionExecutionContext;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.WorkflowTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Action de franchissement d'une étape : positionne l'état de destination et clôt l'instance
 * lorsque cet état est terminal.
 *
 * <p>Un circuit se termine de deux façons, et les deux se valent : par une <b>action</b> déclarée
 * terminale, qui ne mène nulle part, ou en atteignant une <b>étape</b> qui n'offre aucune action.
 * La seconde est la plus lisible pour qui monte un circuit — une étape « Clôturer » dit la fin
 * mieux qu'une case à cocher sur un bouton — et c'est ainsi que sont écrits les circuits livrés :
 * l'étape « Clôture » d'une non-conformité et l'étape « Soldée » d'un plan d'action n'ont pas de
 * suite. Seule la première était reconnue : le dossier atteignait son étape de clôture et
 * l'instance y restait « en cours » indéfiniment — jamais achevée, jamais horodatée, et le circuit
 * ne pouvait plus être relancé sur la ressource, un circuit étant réputé déjà ouvert.</p>
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

    private final WorkflowTransitionRepository transitionRepository;

    @Override
    protected void update(ActionExecutionContext<WorkflowValidationInstance, Transition<WorkflowValidationInstance>> pContexte)
            throws WorkflowException {
        super.update(pContexte); // positionne l'état de destination

        WorkflowValidationInstance instance = pContexte.getData();
        Etat destination = pContexte.getTransition().getEtatDestination();

        if (clotLeCircuit(destination)) {
            instance.setStatus(ValidationStatus.TERMINE);
            instance.setCompletedAt(LocalDateTime.now());
        } else {
            instance.setStatus(ValidationStatus.EN_COURS);
        }
    }

    /**
     * L'état atteint met-il fin au parcours du dossier ?
     *
     * <p>Soit c'est l'état de sortie synthétisé pour une action terminale, soit c'est une étape
     * réelle dont personne ne peut plus rien faire sortir.</p>
     */
    private boolean clotLeCircuit(Etat destination) {
        String code = destination.getCode();
        if (code == null) {
            return false;
        }
        if (code.startsWith(PREFIXE_ETAT_TERMINAL)) {
            return true;
        }
        Long etape = identifiantDEtape(code);
        return etape != null && !transitionRepository.existsByFromStepId(etape);
    }

    /**
     * Identifiant de l'étape que désigne un code d'état, ou {@code null} si le code n'en est pas un.
     *
     * <p>Le moteur nomme ses états par l'identifiant de l'étape correspondante — un code qui n'est
     * pas un nombre ne désigne aucune étape, et n'a donc pas d'action à chercher.</p>
     */
    private Long identifiantDEtape(String code) {
        try {
            return Long.valueOf(code);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
