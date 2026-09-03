package com.qualiapproche.amelioration.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.utils.ReglagesOrganisation;
import com.qualiapproche.amelioration.service.impl.NotificationsDeLUtilisateurService;
import com.qualiapproche.common.dto.NotificationDto;
import com.qualiapproche.common.dto.WorkflowStateDto;
import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.common.enumeration.SourceNotification;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.utils.StatutEnum;

/**
 * Ce que la cloche annonce à l'utilisateur connecté.
 *
 * <p>La liste est recalculée à chaque demande : ces cas fixent ce qu'elle contient, et surtout ce
 * qu'elle fait quand le moteur de circuit ne répond pas — une cloche en échec vaut moins qu'une
 * cloche incomplète.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationsDeLUtilisateurTest {

    private static final String MOI = "agent-1";
    private static final String MON_COURRIEL = "agent-1@exemple.bf";

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PlanActionRepository planActionRepository;
    @Mock private WorkflowClient workflowClient;
    @Mock private ReglagesOrganisation reglagesOrganisation;

    @InjectMocks private NotificationsDeLUtilisateurService service;

    @BeforeEach
    void connecter() {
        Jwt jeton = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject(MOI).claim("email", MON_COURRIEL).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jeton));
        lenient().when(workflowClient.ressourcesADecider("NON_CONFORMITE")).thenReturn(List.of());
        lenient().when(workflowClient.ressourcesADecider("PLAN_ACTION")).thenReturn(List.of());
        lenient().when(planActionRepository
                .findPlanActionsByResponsableEmailAndStatus(MON_COURRIEL, StatutEnum.NON_TRAITER))
                .thenReturn(List.of());
        lenient().when(reglagesOrganisation.entier(anyString(), anyLong())).thenReturn(2L);
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private static WorkflowStateDto etape(String nom) {
        WorkflowStateDto etat = new WorkflowStateDto();
        etat.setCurrentStateName(nom);
        return etat;
    }

    @Test
    @DisplayName("Les décisions ouvertes se ventilent par étape, nommée par le moteur")
    void decisionsOuvertes_ventileesParEtape() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        when(workflowClient.ressourcesADecider("NON_CONFORMITE")).thenReturn(List.of(a, b, c));
        when(workflowClient.getWorkflowStates(List.of(a, b, c))).thenReturn(Map.of(
                a, etape("Validation RQ"), b, etape("Validation RQ"), c, etape("Réception")));

        List<NotificationDto> lignes = service.pourLAppelant();

        // Les intitulés d'étape sont paramétrables : les tenir ici en dur aurait cessé d'être exact
        // à la première étape ajoutée au circuit.
        assertThat(lignes).extracting(NotificationDto::getTitre)
                .containsExactlyInAnyOrder("Validation RQ", "Réception");
        assertThat(lignes).allMatch(l -> l.getGravite() == GraviteNotification.ATTENTION);
    }

    @Test
    @DisplayName("La ligne compte les dossiers, et non les lignes de la cloche")
    void ligne_porteLeNombreDeDossiers() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(workflowClient.ressourcesADecider("NON_CONFORMITE")).thenReturn(List.of(a, b));
        when(workflowClient.getWorkflowStates(List.of(a, b)))
                .thenReturn(Map.of(a, etape("Clôture"), b, etape("Clôture")));

        List<NotificationDto> lignes = service.pourLAppelant();

        // La pastille en fait la somme. Elle comptait ses propres lignes, et annonçait « 1 » pour
        // une catégorie qui en portait deux.
        assertThat(lignes).singleElement()
                .satisfies(l -> assertThat(l.getNombre()).isEqualTo(2));
    }

    @Test
    @DisplayName("Une étape que le moteur ne nomme pas ne fait pas disparaître le dossier")
    void etapeSansNom_dossierConserve() {
        UUID a = UUID.randomUUID();
        when(workflowClient.ressourcesADecider("NON_CONFORMITE")).thenReturn(List.of(a));
        when(workflowClient.getWorkflowStates(List.of(a))).thenReturn(Map.of());

        List<NotificationDto> lignes = service.pourLAppelant();

        // Le dossier attend une décision : l'omettre parce que son étape est sans intitulé le
        // rendrait invisible à celui-là même qui doit le traiter.
        assertThat(lignes).singleElement()
                .satisfies(l -> assertThat(l.getNombre()).isEqualTo(1));
    }

    @Test
    @DisplayName("Le moteur injoignable retire ses lignes, sans faire échouer la cloche")
    void moteurInjoignable_clocheDegradee() {
        when(workflowClient.ressourcesADecider("NON_CONFORMITE"))
                .thenThrow(new IllegalStateException("workflow-service injoignable"));
        when(nonConformiteRepository.countByCreatedByIdAndStatus(MOI, Status.DRAFT)).thenReturn(2L);

        List<NotificationDto> lignes = service.pourLAppelant();

        // Ce que le module sait par lui-même reste annoncé. Une cloche en erreur se lit comme une
        // panne de l'application entière, pour une source secondaire indisponible.
        assertThat(lignes).singleElement()
                .satisfies(l -> assertThat(l.getCode())
                        .isEqualTo(NotificationsDeLUtilisateurService.CODE_BROUILLON));
    }

    @Test
    @DisplayName("Les brouillons de l'appelant informent, ils ne réclament rien")
    void brouillons_informent() {
        when(nonConformiteRepository.countByCreatedByIdAndStatus(MOI, Status.DRAFT)).thenReturn(1L);

        List<NotificationDto> lignes = service.pourLAppelant();

        assertThat(lignes).singleElement().satisfies(l -> {
            assertThat(l.getGravite()).isEqualTo(GraviteNotification.INFO);
            // Un seul dossier : la phrase s'accorde côté serveur, l'écran l'affiche telle quelle.
            assertThat(l.getDetail()).isEqualTo("Une non-conformité reste à finaliser.");
        });
    }

    @Test
    @DisplayName("Rien en attente : la cloche ne rend aucune ligne")
    void rienEnAttente_aucuneLigne() {
        when(nonConformiteRepository.countByCreatedByIdAndStatus(MOI, Status.DRAFT)).thenReturn(0L);

        assertThat(service.pourLAppelant()).isEmpty();
    }

    @Test
    @DisplayName("Les plans d'action à décider forment leur propre ligne")
    void plansAction_ligneDistincte() {
        when(workflowClient.ressourcesADecider("PLAN_ACTION"))
                .thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));

        List<NotificationDto> lignes = service.pourLAppelant();

        assertThat(lignes).singleElement().satisfies(l -> {
            assertThat(l.getCode()).isEqualTo(NotificationsDeLUtilisateurService.CODE_PLAN_A_DECIDER);
            assertThat(l.getNombre()).isEqualTo(2);
        });
    }

    private PlanAction planEcheant(LocalDate echeance) {
        PlanAction plan = new PlanAction();
        plan.setDateEcheance(echeance);
        return plan;
    }

    private void mesPlans(PlanAction... plans) {
        lenient().when(planActionRepository
                .findPlanActionsByResponsableEmailAndStatus(MON_COURRIEL, StatutEnum.NON_TRAITER))
                .thenReturn(List.of(plans));
    }

    @Test
    @DisplayName("Un plan dont l'échéance est passée est annoncé comme urgent")
    void echeanceDepassee_urgente() {
        mesPlans(planEcheant(LocalDate.now().minusDays(3)));

        List<NotificationDto> lignes = service.pourLAppelant();

        // La relance par courriel ne part qu'une fois : qui l'a manquée n'avait plus rien qui le
        // lui rappelle. La cloche le redit tant que le plan n'est pas traité.
        assertThat(lignes).singleElement().satisfies(l -> {
            assertThat(l.getCode()).isEqualTo(NotificationsDeLUtilisateurService.CODE_ECHEANCE_DEPASSEE);
            assertThat(l.getGravite()).isEqualTo(GraviteNotification.URGENT);
        });
    }

    @Test
    @DisplayName("Le retard et l'échéance proche font deux lignes distinctes")
    void retardEtEcheanceProche_deuxLignes() {
        mesPlans(planEcheant(LocalDate.now().minusDays(1)), planEcheant(LocalDate.now().plusDays(1)));

        List<NotificationDto> lignes = service.pourLAppelant();

        // Les fondre aurait noyé le retard dans l'échéance proche, alors que les deux n'appellent
        // pas le même geste.
        assertThat(lignes).extracting(NotificationDto::getCode).containsExactlyInAnyOrder(
                NotificationsDeLUtilisateurService.CODE_ECHEANCE_DEPASSEE,
                NotificationsDeLUtilisateurService.CODE_ECHEANCE_PROCHE);
    }

    @Test
    @DisplayName("Un plan encore loin de son terme n'encombre pas la cloche")
    void echeanceLointaine_tue() {
        mesPlans(planEcheant(LocalDate.now().plusDays(30)));

        assertThat(service.pourLAppelant()).isEmpty();
    }

    @Test
    @DisplayName("Un plan sans échéance n'est ni en retard ni proche")
    void planSansEcheance_ignore() {
        // Le compter d'un côté ou de l'autre inventerait une date que personne n'a saisie.
        mesPlans(planEcheant(null));

        assertThat(service.pourLAppelant()).isEmpty();
    }

    @Test
    @DisplayName("Le seuil de rappel est celui de l'organisation, non une valeur figée")
    void seuil_vientDesReglages() {
        lenient().when(reglagesOrganisation.entier(anyString(), anyLong())).thenReturn(10L);
        mesPlans(planEcheant(LocalDate.now().plusDays(7)));

        // À deux jours de seuil, ce plan serait resté muet. Deux avertissements qui ne se
        // déclencheraient pas au même moment se contrediraient.
        assertThat(service.pourLAppelant()).singleElement()
                .satisfies(l -> assertThat(l.getCode())
                        .isEqualTo(NotificationsDeLUtilisateurService.CODE_ECHEANCE_PROCHE));
    }

    @Test
    @DisplayName("Chaque ligne dit de quel module elle vient")
    void chaqueLigne_porteSaSource() {
        when(nonConformiteRepository.countByCreatedByIdAndStatus(MOI, Status.DRAFT)).thenReturn(1L);
        mesPlans(planEcheant(LocalDate.now().minusDays(1)));

        // La cloche est unique, ses sources ne le sont pas : sans ce repère, l'écran ne saurait pas
        // regrouper ni dire quel module est momentanément muet.
        assertThat(service.pourLAppelant())
                .allMatch(l -> l.getSource() == SourceNotification.AMELIORATION);
    }
}
