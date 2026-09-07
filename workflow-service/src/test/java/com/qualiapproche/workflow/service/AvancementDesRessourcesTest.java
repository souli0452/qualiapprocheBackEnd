package com.qualiapproche.workflow.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualiapproche.common.enumeration.AvancementCircuit;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
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

/**
 * Où en est un dossier, dit par le moteur et par lui seul.
 *
 * <p>Les tableaux de bord des modules métier déduisaient cela d'un état inscrit sur le dossier —
 * {@code CLOTURE}, {@code DRAFT}. C'était recopier une partie du circuit dans chaque module : le
 * chiffre devenait faux dès qu'une étape était ajoutée ou renommée. Ces cas fixent la réponse que
 * le moteur rend à leur place, et surtout ce qui distingue un circuit <b>ouvert</b> d'un circuit
 * <b>engagé</b> — l'instance naît avec le dossier, son existence ne dit donc pas qu'il a été
 * soumis.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvancementDesRessourcesTest {

    @Mock private IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> moteur;
    @Mock private ValidationHistoryRepository historyRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WorkflowRepository workflowRepository;
    @Mock private WorkflowValidationInstanceRepository validationInstanceRepository;
    @Mock private WorkflowStepFieldRepository stepFieldRepository;
    @Mock private WorkflowFieldValueRepository fieldValueRepository;
    @Mock private WorkflowTransitionRepository transitionRepository;
    @Mock private WorkflowStepRepository stepRepository;

    private WorkflowService service;

    private static final String MOI = "agent-1";

    @BeforeEach
    void setUp() {
        service = new WorkflowService(moteur, historyRepository, eventPublisher, workflowRepository,
                validationInstanceRepository, stepFieldRepository, fieldValueRepository,
                transitionRepository, stepRepository,
                org.mockito.Mockito.mock(StructureUtilisateurService.class),
                null);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("jeton").header("alg", "none").subject(MOI).build()));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private WorkflowValidationInstance instance(UUID ressource, ValidationStatus statut) {
        return WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(ressource.toString())
                .resourceType("NON_CONFORMITE")
                .workflowCode(UUID.randomUUID().toString())
                .etatCode("10")
                .status(statut)
                .createurId(MOI)
                .build();
    }

    /** Déclare les décisions prises : seules les instances citées ont franchi une étape. */
    private void decisionsPrises(WorkflowValidationInstance... engagees) {
        when(historyRepository.instancesAyantUneDecision(anyCollection()))
                .thenReturn(java.util.Arrays.stream(engagees)
                        .map(WorkflowValidationInstance::getId).toList());
    }

    @Test
    @DisplayName("Un circuit ouvert sur lequel rien n'a été décidé n'est pas engagé")
    void circuitSansDecision_nonEngage() {
        UUID brouillon = UUID.randomUUID();
        UUID soumis = UUID.randomUUID();
        WorkflowValidationInstance instanceSoumise = instance(soumis, ValidationStatus.EN_COURS);
        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of(instance(brouillon, ValidationStatus.EN_COURS), instanceSoumise));
        decisionsPrises(instanceSoumise);

        Map<UUID, AvancementCircuit> avancements =
                service.avancementDesRessources(List.of(brouillon, soumis));

        // L'instance naît avec le dossier : sa seule existence ne dit pas qu'il a été soumis.
        assertThat(avancements.get(brouillon)).isEqualTo(AvancementCircuit.NON_ENGAGE);
        assertThat(avancements.get(soumis)).isEqualTo(AvancementCircuit.EN_COURS);
    }

    @Test
    @DisplayName("Un circuit terminé l'est, sans qu'on ait à nommer son étape de clôture")
    void circuitTermine() {
        UUID clos = UUID.randomUUID();
        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of(instance(clos, ValidationStatus.TERMINE)));
        decisionsPrises();

        assertThat(service.avancementDesRessources(List.of(clos)))
                .containsEntry(clos, AvancementCircuit.TERMINE);
    }

    @Test
    @DisplayName("Une ressource sans circuit est rendue non engagée, et non omise")
    void ressourceSansCircuit_rendueQuandMeme() {
        UUID orpheline = UUID.randomUUID();
        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of());

        // Le tableau de bord compte alors ce qu'il a demandé, et non ce que le moteur connaît :
        // une réponse amputée aurait fait mentir le total sans que rien ne le signale.
        assertThat(service.avancementDesRessources(List.of(orpheline)))
                .containsEntry(orpheline, AvancementCircuit.NON_ENGAGE);
        verify(historyRepository, never()).instancesAyantUneDecision(anyCollection());
    }

    @Test
    @DisplayName("Un lot trop grand est refusé, comme pour la lecture d'état")
    void lotTropGrand_refuse() {
        List<UUID> trop = java.util.stream.Stream
                .generate(UUID::randomUUID).limit(WorkflowService.TAILLE_LOT_MAX + 1L).toList();

        assertThatThrownBy(() -> service.avancementDesRessources(trop))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Mes dossiers ouverts distinguent ce que je n'ai pas soumis de ce que j'attends")
    void mesDossiers_brouillonsEtEnCours() {
        UUID brouillon = UUID.randomUUID();
        UUID soumis = UUID.randomUUID();
        WorkflowValidationInstance instanceSoumise = instance(soumis, ValidationStatus.EN_COURS);
        when(validationInstanceRepository.findByResourceTypeAndCreateurIdAndStatus(
                "NON_CONFORMITE", MOI, ValidationStatus.EN_COURS))
                .thenReturn(List.of(instance(brouillon, ValidationStatus.EN_COURS), instanceSoumise));
        decisionsPrises(instanceSoumise);

        Map<UUID, AvancementCircuit> miens = service.mesDossiersOuverts("NON_CONFORMITE");

        assertThat(miens).containsEntry(brouillon, AvancementCircuit.NON_ENGAGE)
                .containsEntry(soumis, AvancementCircuit.EN_COURS);
    }

    @Test
    @DisplayName("Sans utilisateur connecté, aucun dossier n'est rendu")
    void sansAppelant_aucunDossier() {
        SecurityContextHolder.clearContext();

        assertThat(service.mesDossiersOuverts("NON_CONFORMITE")).isEmpty();
        verify(validationInstanceRepository, never())
                .findByResourceTypeAndCreateurIdAndStatus(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any());
    }
}
