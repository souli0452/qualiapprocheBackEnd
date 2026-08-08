package com.qualiapproche.workflow.controller;

import com.qualiapproche.common.config.PermissionConfig;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.dto.WorkflowDto;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import com.qualiapproche.workflow.repository.WorkflowFieldValueRepository;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import com.qualiapproche.workflow.repository.WorkflowStepFieldRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowTransitionRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import com.qualiapproche.workflow.service.WorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.qualiapproche.common.annotation.RequirePermissions;

/**
 * Protection de l'administration des circuits.
 *
 * <p>Jusqu'ici aucune de ces trois opérations n'était protégée : tout utilisateur authentifié
 * pouvait réécrire ou supprimer le circuit de validation de l'organisation. Ce test vérifie à la
 * fois la règle métier (seul {@code workflow-write} passe) et son câblage — le bean {@code perm}
 * doit être présent dans ce service, et {@code @RequirePermissions} doit rester lisible à travers
 * le proxy posé par la sécurité de méthode.</p>
 *
 * <p>Le service est réel, ses seuls dépôts étant mockés : le JDK de compilation est plus récent
 * que le Byte Buddy embarqué, qui ne sait donc pas mocker de classe. Le test y gagne — c'est bien
 * le chemin contrôleur → service qui est exercé.</p>
 */
@SpringJUnitConfig(WorkflowControllerAutorisationTest.ConfigurationDeTest.class)
class WorkflowControllerAutorisationTest {

    @Configuration
    @EnableMethodSecurity
    @Import(PermissionConfig.class)
    static class ConfigurationDeTest {

        @Bean WorkflowRepository workflowRepository() { return Mockito.mock(WorkflowRepository.class); }
        @Bean WorkflowValidationInstanceRepository validationInstanceRepository() { return Mockito.mock(WorkflowValidationInstanceRepository.class); }
        @Bean ValidationHistoryRepository historyRepository() { return Mockito.mock(ValidationHistoryRepository.class); }
        @Bean WorkflowStepFieldRepository stepFieldRepository() { return Mockito.mock(WorkflowStepFieldRepository.class); }
        @Bean WorkflowFieldValueRepository fieldValueRepository() { return Mockito.mock(WorkflowFieldValueRepository.class); }
        @Bean WorkflowTransitionRepository transitionRepository() { return Mockito.mock(WorkflowTransitionRepository.class); }
        @Bean WorkflowStepRepository stepRepository() { return Mockito.mock(WorkflowStepRepository.class); }
        @Bean ApplicationEventPublisher eventPublisher() { return Mockito.mock(ApplicationEventPublisher.class); }

        @Bean
        @SuppressWarnings("unchecked")
        IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> moteur() {
            return Mockito.mock(IWorkflowEnginePort.class);
        }

        @Bean
        WorkflowService workflowService(
                IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> pMoteur,
                ValidationHistoryRepository pHistory, ApplicationEventPublisher pPublisher,
                WorkflowRepository pWorkflows, WorkflowValidationInstanceRepository pInstances,
                WorkflowStepFieldRepository pStepFields, WorkflowFieldValueRepository pFieldValues,
                WorkflowTransitionRepository pTransitions, WorkflowStepRepository pSteps) {
            // Pas de proxy ici : ce contexte de test ne monte pas la gestion transactionnelle.
            // Voir WorkflowService#self().
            return new WorkflowService(pMoteur, pHistory, pPublisher, pWorkflows, pInstances,
                    pStepFields, pFieldValues, pTransitions, pSteps,
                    org.mockito.Mockito.mock(com.qualiapproche.workflow.service.StructureUtilisateurService.class),
                    null);
        }

        @Bean
        WorkflowController workflowController(WorkflowService pService) {
            return new WorkflowController(pService);
        }
    }

    @Autowired private WorkflowController controller;
    @Autowired private WorkflowRepository workflowRepository;
    @Autowired private WorkflowValidationInstanceRepository validationInstanceRepository;

    private final UUID workflowId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Mockito.reset(workflowRepository, validationInstanceRepository);
        Workflow aCircuit = Workflow.builder().nom("Validation standard").resourceType("DOCUMENT").actif(true).build();
        aCircuit.setId(workflowId);
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(aCircuit));
        when(validationInstanceRepository.existsByEtatCodeInAndStatus(anyList(), any())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authentifierAvec(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agent-qualite", "n/a",
                        Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }

    /** Vérifie que la sécurité laisse passer : l'issue métier de l'appel n'est pas le sujet. */
    private void assertNonBloqueParLaSecurite(Executable pAppel) {
        try {
            pAppel.execute();
        } catch (AccessDeniedException e) {
            throw new AssertionError("L'appel a été refusé par la sécurité alors qu'il devait passer.", e);
        } catch (Throwable ignored) {
            // Échec métier (dépôt mocké, dossier absent…) : hors sujet ici.
        }
    }

    // ------------------------------------------------------------------ refus

    @Test
    @DisplayName("Un utilisateur authentifié sans permission ne peut pas créer de circuit")
    void creation_sansPermission_refusee() {
        authentifierAvec("workflow-read");

        assertThatThrownBy(() -> controller.createWorkflow(new WorkflowDto()))
                .isInstanceOf(AccessDeniedException.class);
        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un utilisateur authentifié sans permission ne peut pas modifier de circuit")
    void modification_sansPermission_refusee() {
        authentifierAvec("workflow-read");

        assertThatThrownBy(() -> controller.updateWorkflow(workflowId, new WorkflowDto()))
                .isInstanceOf(AccessDeniedException.class);
        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un utilisateur authentifié sans permission ne peut pas supprimer de circuit")
    void suppression_sansPermission_refusee() {
        authentifierAvec("workflow-read");

        assertThatThrownBy(() -> controller.deleteWorkflow(workflowId))
                .isInstanceOf(AccessDeniedException.class);
        verify(workflowRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Un appelant anonyme ne peut pas supprimer de circuit")
    void suppression_anonyme_refusee() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("cle", "anonyme",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> controller.deleteWorkflow(workflowId))
                .isInstanceOf(AccessDeniedException.class);
        verify(workflowRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------ autorisation

    @Test
    @DisplayName("La permission workflow-write autorise la suppression")
    void suppression_avecWorkflowWrite_autorisee() {
        authentifierAvec("workflow-write");

        controller.deleteWorkflow(workflowId);

        verify(workflowRepository).delete(any());
    }

    @Test
    @DisplayName("La permission workflow-write autorise la création")
    void creation_avecWorkflowWrite_autorisee() {
        authentifierAvec("workflow-write");

        controller.createWorkflow(WorkflowDto.builder().nom("Validation standard")
                .resourceType("DOCUMENT").build());

        verify(workflowRepository).save(any());
    }

    @Test
    @DisplayName("La permission est reconnue quelle que soit sa casse")
    void administration_permissionEnMajuscules_autorisee() {
        authentifierAvec("WORKFLOW-WRITE");

        controller.deleteWorkflow(workflowId);

        verify(workflowRepository).delete(any());
    }

    // ------------------------------------------------------------------ non-régression inter-services

    @Test
    @DisplayName("Le pilotage des instances reste ouvert : il est appelé de service à service, "
            + "sans en-tête de permissions, et son habilitation est portée par les transitions")
    void pilotageDesInstances_nonSoumisAuxPermissions() {
        authentifierAvec("aucune-permission-utile");

        assertNonBloqueParLaSecurite(() -> controller.getAllWorkflows());
        assertNonBloqueParLaSecurite(() -> controller.getWorkflowState(UUID.randomUUID()));
        assertNonBloqueParLaSecurite(() -> controller.getLastValidationInstance(UUID.randomUUID()));
        assertNonBloqueParLaSecurite(() -> controller.initiateWorkflow(UUID.randomUUID(), "DOCUMENT", workflowId, null, null));
        assertNonBloqueParLaSecurite(() -> controller.validateStep(UUID.randomUUID(), null));
        assertNonBloqueParLaSecurite(() -> controller.rejectStep(UUID.randomUUID(), null));
    }

    @Test
    @DisplayName("Le contrôleur porte bien la déclaration de permissions, y compris une fois proxyfié")
    void declarationDePermissions_lisibleAtraversLeProxy() {
        var aAnnotation = controller.getClass()
                .getAnnotation(RequirePermissions.class);

        assertThat(aAnnotation)
                .as("sans cette annotation, PermissionChecker refuse tout en silence")
                .isNotNull();
        assertThat(aAnnotation.create()).containsExactly("workflow-write");
        assertThat(aAnnotation.update()).containsExactly("workflow-write");
        assertThat(aAnnotation.delete()).containsExactly("workflow-write");
    }
}
