package com.qualiapproche.workflow.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.model.FaitsDuDossier;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Ce qu'on répond à qui demande une action que le dossier n'admet pas encore.
 *
 * <p>L'action ne disparaît plus de l'écran quand sa condition n'est pas remplie : elle est offerte,
 * et c'est sa demande qui est refusée. Tout repose donc sur ce que dit le refus. Il opposait le nom
 * technique du fait — « la condition PLANS_ACTION_SOLDES n'est pas remplie » — qui ne désigne rien
 * pour un responsable qualité et ne lui dit pas quoi faire. Le circuit porte pourtant déjà
 * l'explication en clair, écrite par l'auteur du circuit au moment où il a posé la condition.</p>
 */
class RefusDeConditionExpliqueTest {

    /** Le strict nécessaire pour atteindre {@code verifierConditionMetier}. */
    private static final class ServiceDeTest extends AbstractWorkflowService<WorkflowValidationInstance> {

        @SuppressWarnings("unchecked")
        ServiceDeTest() {
            super(mock(IWorkflowEnginePort.class), mock(ValidationHistoryRepository.class),
                    mock(ApplicationEventPublisher.class));
        }

        @Override
        protected JpaRepository<WorkflowValidationInstance, UUID> getRepository() {
            return null;
        }

        @Override
        protected String getWorkflowCode() {
            return "NON_CONFORMITE";
        }

        @Override
        protected String getCurrentUserId() {
            return "utilisateur";
        }

        void verifier(WorkflowValidationInstance dossier, TransitionPersistante transition) {
            verifierConditionMetier(dossier, transition);
        }
    }

    private final ServiceDeTest service = new ServiceDeTest();

    private WorkflowValidationInstance dossier(String... faits) {
        return WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(UUID.randomUUID().toString())
                .resourceType("NON_CONFORMITE")
                .faits(FaitsDuDossier.ecrire(Set.of(faits)))
                .build();
    }

    private TransitionPersistante cloture(String libelleCondition) {
        TransitionPersistante transition =
                new TransitionPersistante("1", new Etat("8"), new Etat("9"));
        transition.setLibelle("Clôturer la NC");
        transition.setConditionRequise("PLANS_ACTION_SOLDES");
        transition.setConditionLibelle(libelleCondition);
        return transition;
    }

    @Test
    @DisplayName("Le refus reprend l'explication en clair du circuit, et nomme l'action demandée")
    void conditionNonRemplie_refusEnClair() {
        assertThatThrownBy(() -> service.verifier(dossier(), cloture(
                "toutes les actions correctives du dossier ont été réalisées, vérifiées et "
                        + "reconnues efficaces")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Clôturer la NC")
                .hasMessageContaining("toutes les actions correctives du dossier ont été réalisées")
                // Le nom technique du fait n'a pas à figurer : il ne désigne rien pour le lecteur.
                .hasMessageNotContaining("PLANS_ACTION_SOLDES")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Sans explication écrite, le nom du fait reste préférable au silence")
    void sansLibelle_repliSurLeNomDuFait() {
        assertThatThrownBy(() -> service.verifier(dossier(), cloture(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PLANS_ACTION_SOLDES");
    }

    @Test
    @DisplayName("Le dossier qui remplit la condition passe sans refus")
    void conditionRemplie_aucunRefus() {
        service.verifier(dossier("PLANS_ACTION_SOLDES"), cloture("peu importe"));
    }

    @Test
    @DisplayName("Une transition sans condition ne refuse jamais")
    void sansCondition_aucunRefus() {
        TransitionPersistante libre = new TransitionPersistante("2", new Etat("8"), new Etat("9"));
        libre.setLibelle("Approuver");

        service.verifier(dossier(), libre);
    }
}
