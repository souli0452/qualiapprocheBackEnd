package com.qualiapproche.amelioration.controller;

import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteResumeMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.NonConformiteService;
import com.qualiapproche.amelioration.service.PlanActionService;
import com.qualiapproche.amelioration.service.impl.AccesAuxDossiers;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlanActionServiceImpl;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.utils.StatutEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Un plan d'action se voit là où se voit sa non-conformité.
 *
 * <p>La fiche d'un plan joint la non-conformité entière qui le motive, et ni la fiche, ni les
 * listes, ni la correction, ni la suppression d'un plan ne regardaient la structure de
 * l'appelant : la borne posée sur les dossiers se contournait par leurs plans. Ces tests fixent la
 * règle — celle des dossiers, sans en inventer une autre.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlansDActionBornesAuDossierTest {

    @Mock private PlanActionRepository planActionRepository;
    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private NonConformiteResumeMapper nonConformiteResumeMapper;
    @Mock private PieceJointeStockageService fichierService;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;
    @Mock private WorkflowClient workflowClient;
    @Mock private PermissionChecker permissionChecker;
    @Mock private NonConformiteService nonConformiteService;

    @InjectMocks private PlanActionServiceImpl service;

    private final Pageable page = PageRequest.of(0, 10);
    private final UUID agentGsi = UUID.randomUUID();

    /** Un dossier de DQA, que l'agent de GSI connecté ne peut pas consulter, et l'un de ses plans. */
    private final NonConformite dossierDeDqa = new NonConformite();
    private final PlanAction planDeDqa = new PlanAction();

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(agentGsi.toString())
                .claim("structure_id", "GSI")
                .claim("email", "Agent.Gsi@exemple.bf")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        dossierDeDqa.setId(UUID.randomUUID());
        dossierDeDqa.setStructureSoumissionId("DQA");
        planDeDqa.setId(UUID.randomUUID());
        planDeDqa.setNonConformeId(dossierDeDqa.getId());

        when(nonConformiteRepository.findById(dossierDeDqa.getId())).thenReturn(Optional.of(dossierDeDqa));
        when(planActionRepository.findById(planDeDqa.getId())).thenReturn(Optional.of(planDeDqa));
        when(nonConformiteService.peutConsulter(dossierDeDqa)).thenReturn(false);
        when(permissionChecker.detient(any(String[].class))).thenReturn(false);
        when(planActionRepository.findVisiblesPar(any(), any(), any(), any())).thenReturn(Page.empty(page));
        when(planActionRepository.findAll(any(Pageable.class))).thenReturn(Page.empty(page));
        when(planActionRepository.findPlanActionsByResponsableEmail(anyString(), any(Pageable.class)))
                .thenReturn(Page.empty(page));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private static void assertStatut(org.assertj.core.api.ThrowableAssert.ThrowingCallable appel, int statut) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(statut));
    }

    // ------------------------------------------------------------------ un plan par son identifiant

    @Test
    @DisplayName("Le plan d'un dossier d'une autre structure est introuvable : ni lu, ni corrigé, ni supprimé")
    void plan_d_une_autre_structure_introuvable() {
        PlanActionService plans = mock(PlanActionService.class);
        AccesAuxDossiers acces = new AccesAuxDossiers(
                nonConformiteRepository, planActionRepository, nonConformiteService, permissionChecker);
        PlanActionController controleur = new PlanActionController(plans, workflowClient, acces);
        PlanActionDto correction = new PlanActionDto();
        correction.setId(planDeDqa.getId());

        assertStatut(() -> controleur.getById(planDeDqa.getId()), 404);
        assertStatut(() -> controleur.corriger(correction), 404);
        assertStatut(() -> controleur.deleteyId(planDeDqa.getId()), 404);

        // Rien n'atteint le service : ni la lecture qui joint le dossier, ni l'écriture.
        verifyNoInteractions(plans);
    }

    // ------------------------------------------------------------------ la création d'un plan

    private PlanActionController controleurSurLeVraiService() {
        AccesAuxDossiers acces = new AccesAuxDossiers(
                nonConformiteRepository, planActionRepository, nonConformiteService, permissionChecker);
        return new PlanActionController(service, workflowClient, acces);
    }

    @Test
    @DisplayName("Créer un plan sur le dossier d'une autre structure en s'en désignant responsable répond 404, sans rien écrire")
    void creer_un_plan_sur_un_dossier_d_une_autre_structure_introuvable() {
        PlanActionDto plan = new PlanActionDto();
        plan.setNonConformeId(dossierDeDqa.getId());
        plan.setResponsableId(agentGsi.toString());

        assertStatut(() -> controleurSurLeVraiService().create(plan), 404);

        // Ni plan enregistré, ni clôture rouverte, ni résumé du dossier rendu : l'agent ne peut
        // donc pas devenir « responsable d'un plan » d'un dossier qu'il ne voit pas.
        verify(planActionRepository, never()).save(any());
        verifyNoInteractions(plansActionService, nonConformiteResumeMapper, planActionMapper);
    }

    @Test
    @DisplayName("Créer un plan sans désigner de dossier répond 400")
    void creer_un_plan_sans_dossier_refuse() {
        assertStatut(() -> controleurSurLeVraiService().create(new PlanActionDto()), 400);

        verify(planActionRepository, never()).save(any());
        verifyNoInteractions(plansActionService);
    }

    @Test
    @DisplayName("Créer un plan sur un dossier que l'on consulte reste possible")
    void creer_un_plan_sur_un_dossier_consultable() throws Exception {
        NonConformite dossierDeGsi = new NonConformite();
        dossierDeGsi.setId(UUID.randomUUID());
        dossierDeGsi.setStructureSoumissionId("GSI");
        when(nonConformiteRepository.findById(dossierDeGsi.getId())).thenReturn(Optional.of(dossierDeGsi));
        when(nonConformiteService.peutConsulter(dossierDeGsi)).thenReturn(true);
        PlanActionService plans = mock(PlanActionService.class);
        PlanActionController controleur = new PlanActionController(plans, workflowClient,
                new AccesAuxDossiers(nonConformiteRepository, planActionRepository, nonConformiteService, permissionChecker));
        PlanActionDto plan = new PlanActionDto();
        plan.setNonConformeId(dossierDeGsi.getId());
        when(plans.createPlanActionDto(plan)).thenReturn(plan);

        assertThat(controleur.create(plan).getStatusCode().value()).isEqualTo(200);
        verify(plans).createPlanActionDto(plan);
    }

    @Test
    @DisplayName("Créer un plan avec l'identifiant d'un plan existant en crée un nouveau, sans réécrire l'autre")
    void creer_avec_l_identifiant_d_un_plan_existant_ne_le_reecrit_pas() throws Exception {
        NonConformite dossierDeGsi = new NonConformite();
        dossierDeGsi.setId(UUID.randomUUID());
        when(nonConformiteRepository.findById(dossierDeGsi.getId())).thenReturn(Optional.of(dossierDeGsi));
        when(nonConformiteService.peutConsulter(dossierDeGsi)).thenReturn(true);

        PlanActionDto saisie = new PlanActionDto();
        saisie.setId(planDeDqa.getId());
        saisie.setNonConformeId(dossierDeGsi.getId());
        // Ce que le mapper recopie de la saisie : l'identifiant du plan visé, un circuit, un statut
        // soldé et une traçabilité forgée.
        PlanAction recopie = new PlanAction();
        recopie.setId(planDeDqa.getId());
        recopie.setWorkflowId(UUID.randomUUID());
        recopie.setStatus(StatutEnum.TRAITER);
        recopie.setUpdateById("quelqu-un-d-autre");
        when(planActionMapper.toEntity(saisie)).thenReturn(recopie);
        when(planActionRepository.save(any(PlanAction.class))).thenAnswer(i -> i.getArgument(0));
        when(planActionMapper.toDto(any(PlanAction.class))).thenReturn(new PlanActionDto());

        controleurSurLeVraiService().create(saisie);

        ArgumentCaptor<PlanAction> enregistre = ArgumentCaptor.forClass(PlanAction.class);
        verify(planActionRepository).save(enregistre.capture());
        assertThat(enregistre.getValue().getId()).isNull();
        assertThat(enregistre.getValue().getWorkflowId()).isNull();
        assertThat(enregistre.getValue().getStatus()).isEqualTo(StatutEnum.INACTIF);
        assertThat(enregistre.getValue().getUpdateById()).isNull();
        assertThat(enregistre.getValue().getNonConformeId()).isEqualTo(dossierDeGsi.getId());
    }

    // ------------------------------------------------------------------ les listes

    @Test
    @DisplayName("La liste générale des plans est bornée aux dossiers visibles et aux plans confiés")
    void liste_generale_bornee() {
        service.allPlanActions(page);

        verify(planActionRepository).findVisiblesPar(eq("GSI"), eq(agentGsi.toString()), eq(agentGsi), any());
        verify(planActionRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("La portée transverse voit toujours tous les plans")
    void liste_generale_portee_transverse() {
        when(permissionChecker.detient(any(String[].class))).thenReturn(true);

        service.allPlanActions(page);

        verify(planActionRepository).findAll(any(Pageable.class));
        verify(planActionRepository, never()).findVisiblesPar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Les plans d'une autre structure ne se lisent pas par la liste par structure")
    void liste_par_structure_bornee() {
        assertStatut(() -> service.getPlanActionsByStructure("DQA", page), 403);
        verify(planActionRepository, never()).findPlanActionsByStructureId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Les plans confiés à une autre personne ne se lisent pas par son adresse")
    void liste_par_adresse_bornee() {
        assertStatut(() -> service.planActionByResponsableAll("collegue@exemple.bf", page), 403);
        assertStatut(() -> service.planActionByResponsable("collegue@exemple.bf", StatutEnum.values()[0], page), 403);
        verify(planActionRepository, never()).findPlanActionsByResponsableEmail("collegue@exemple.bf", page);

        // Sa propre adresse, quelle que soit la casse saisie, reste lisible comme avant.
        service.planActionByResponsableAll("agent.gsi@exemple.bf", page);
        verify(planActionRepository).findPlanActionsByResponsableEmail("agent.gsi@exemple.bf", page);
    }
}
