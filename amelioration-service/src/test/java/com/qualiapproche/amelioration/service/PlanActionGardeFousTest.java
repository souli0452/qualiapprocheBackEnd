package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.UtilisateurClient;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlanActionServiceImpl;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.utils.StatutEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.qualiapproche.amelioration.client.WorkflowClient;

/**
 * Ce que la fiche peut faire d'un plan d'action, et ce qui relève de son circuit.
 *
 * <p>Trois gestes se ressemblaient à l'écran et n'ont pas les mêmes conséquences : corriger la
 * description d'une action, en changer le responsable, et la supprimer. Les deux derniers, sur un
 * plan déjà confié, défont en silence ce que le circuit a établi — l'un déplace la responsabilité
 * sans que le moteur en sache rien, l'autre efface l'historique des décisions prises.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanActionGardeFousTest {

    @Mock private PlanActionRepository planActionRepository;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private PieceJointeStockageService fichierService;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;
    @Mock private UtilisateurClient utilisateurClient;
    @Mock private WorkflowClient workflowClient;

    @InjectMocks private PlanActionServiceImpl service;

    private final UUID dossier = UUID.randomUUID();
    private final UUID ancienResponsable = UUID.randomUUID();
    private PlanAction plan;

    @BeforeEach
    void planExistant() {
        plan = new PlanAction();
        plan.setId(UUID.randomUUID());
        plan.setNonConformeId(dossier);
        plan.setNumeroOdre("P-A-1");
        plan.setResponsableId(ancienResponsable);
        plan.setResponsableNomComplet("Ancien responsable");
        when(planActionRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(planActionRepository.findPlanActionsByNonConformeId(dossier)).thenReturn(List.of(plan));
        when(planActionRepository.save(any(PlanAction.class))).thenAnswer(i -> i.getArgument(0));
        when(planActionMapper.toDto(any(PlanAction.class))).thenReturn(new PlanActionDto());
    }

    private PlanActionDto correctionAvecResponsable(UUID responsable) {
        PlanActionDto dto = new PlanActionDto();
        dto.setId(plan.getId());
        dto.setResponsableId(responsable == null ? null : responsable.toString());
        return dto;
    }

    @Test
    @DisplayName("Une action en cours de réalisation ne change pas de porteur")
    void actionEnCours_transfertRefuse() {
        plan.setWorkflowId(UUID.randomUUID());
        plan.setStatus(StatutEnum.NON_TRAITER);

        assertThatThrownBy(() -> service.corriger(correctionAvecResponsable(UUID.randomUUID())))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // Son responsable est en train de la mener : la lui retirer en silence laisserait l'ancien
        // décider d'une action dont il ne répond plus.
        assertThat(plan.getResponsableId()).isEqualTo(ancienResponsable);
    }

    @Test
    @DisplayName("Le pilote qui constate la réalisation peut confier l'action à quelqu'un d'autre")
    void enVerification_transfertAccepte() {
        plan.setWorkflowId(UUID.randomUUID());
        plan.setStatus(StatutEnum.EN_VERIFICATION);
        UUID nouveau = UUID.randomUUID();
        when(utilisateurClient.getUserById(anyString()))
                .thenReturn(Map.of("data", Map.of("email", "ada@exemple.fr", "nomComplet", "Ada Lovelace")));

        service.corriger(correctionAvecResponsable(nouveau));

        // Le moteur doit suivre : les étapes réservées au titulaire s'ouvrent à la personne qu'il
        // connaît, non à celle qu'affiche la fiche.
        assertThat(plan.getResponsableId()).isEqualTo(nouveau);
        assertThat(plan.getResponsableNomComplet()).isEqualTo("Ada Lovelace");
        verify(workflowClient).designerTitulaire(plan.getId(), nouveau.toString());
    }

    @Test
    @DisplayName("Un transfert que le circuit n'a pas enregistré est annulé")
    void transfertNonRepercute_annule() {
        plan.setWorkflowId(UUID.randomUUID());
        plan.setStatus(StatutEnum.EN_VERIFICATION);
        when(utilisateurClient.getUserById(anyString()))
                .thenReturn(Map.of("data", Map.of("email", "a@b.fr", "nomComplet", "Ada")));
        org.mockito.Mockito.doThrow(new RuntimeException("workflow-service injoignable"))
                .when(workflowClient).designerTitulaire(any(), anyString());

        // À moitié fait, le transfert serait pire que pas fait du tout : la fiche nommerait l'un et
        // le circuit ouvrirait à l'autre.
        assertThatThrownBy(() -> service.corriger(correctionAvecResponsable(UUID.randomUUID())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("annulé");
    }

    @Test
    @DisplayName("Le compte rendu ne se réécrit plus une fois l'action déclarée réalisée")
    void compteRendu_geleApresRealisation() {
        plan.setWorkflowId(UUID.randomUUID());
        plan.setStatus(StatutEnum.EN_VERIFICATION);
        plan.setSolutionRetenues("Procédure rédigée");

        PlanActionDto dto = new PlanActionDto();
        dto.setId(plan.getId());
        dto.setSolutionRetenues("Tout autre chose");

        service.corriger(dto);

        // C'est sur ce compte rendu que le pilote puis le responsable qualité se prononcent : le
        // laisser réécrire après coup viderait leur avis de son sens.
        assertThat(plan.getSolutionRetenues()).isEqualTo("Procédure rédigée");
    }

    @Test
    @DisplayName("Le responsable d'un plan pas encore confié se change librement")
    void planNonEngage_transfertAccepte() {
        UUID nouveau = UUID.randomUUID();

        service.corriger(correctionAvecResponsable(nouveau));

        // Rien n'est encore engagé : c'est une correction de la proposition, pas un transfert.
        assertThat(plan.getResponsableId()).isEqualTo(nouveau);
    }

    @Test
    @DisplayName("Le constat se rapporte sur une action engagée, sa définition ne se réécrit plus")
    void planEngage_constatSeul() {
        plan.setWorkflowId(UUID.randomUUID());
        plan.setStatus(StatutEnum.NON_TRAITER);
        plan.setActionCorrective("Rédiger la procédure");
        PlanActionDto dto = correctionAvecResponsable(ancienResponsable);
        dto.setSolutionRetenues("Procédure rédigée et diffusée");
        dto.setActionCorrective("Tout autre chose");

        service.corriger(dto);

        // Une action validée par le pilote puis par le responsable qualité ne se reformule pas en
        // silence : ce ne serait plus l'action sur laquelle ils se sont prononcés.
        assertThat(plan.getSolutionRetenues()).isEqualTo("Procédure rédigée et diffusée");
        assertThat(plan.getActionCorrective()).isEqualTo("Rédiger la procédure");
    }

    @Test
    @DisplayName("Tant que l'action est une proposition, son analyse se corrige")
    void propositionCorrigee_analyseComprise() {
        plan.setCauseIdentifiees("Procédure absente");
        plan.setSolutionRetenues("Procédure rédigée");
        plan.setActionCorrective("Rédiger la procédure");

        PlanActionDto dto = correctionAvecResponsable(UUID.randomUUID());
        dto.setCauseIdentifiees("Contrôle non tracé");
        dto.setSolutionRetenues("Fiche de contrôle instaurée");

        service.corriger(dto);

        // Tant que l'action est une proposition, tout s'y corrige, l'analyse comprise : c'est la
        // personne imputée qui recherche la cause et arrête la solution, et son plan doit être
        // entier avant d'être soumis. Elles n'étaient modifiables qu'une fois l'action engagée, si
        // bien qu'un plan encore en discussion ne pouvait pas être repris sur ce qu'il a de plus
        // substantiel.
        assertThat(plan.getCauseIdentifiees()).isEqualTo("Contrôle non tracé");
        assertThat(plan.getSolutionRetenues()).isEqualTo("Fiche de contrôle instaurée");
    }

    @Test
    @DisplayName("Une action engagée n'est plus redéfinie par la fiche")
    void actionEngagee_analyseFigee() {
        plan.setWorkflowId(UUID.randomUUID());
        plan.setStatus(StatutEnum.EN_VERIFICATION);
        plan.setCauseIdentifiees("Procédure absente");
        plan.setActionCorrective("Rédiger la procédure");

        PlanActionDto dto = new PlanActionDto();
        dto.setId(plan.getId());
        dto.setCauseIdentifiees("Autre chose");
        dto.setActionCorrective("Autre chose");

        service.corriger(dto);

        // Ce que le responsable a rapporté est ce sur quoi le pilote puis le responsable qualité se
        // prononcent : le laisser réécrire après coup viderait leur avis de son sens.
        assertThat(plan.getCauseIdentifiees()).isEqualTo("Procédure absente");
        assertThat(plan.getActionCorrective()).isEqualTo("Rédiger la procédure");
    }

    @Test
    @DisplayName("Le rang saisi est repris tel quel")
    void rangSaisi_repris() {
        PlanActionDto dto = new PlanActionDto();
        dto.setId(plan.getId());
        dto.setNumeroOdre("3");

        service.corriger(dto);

        // C'est un repère de lecture parmi les plans du dossier, non une clé : deux actions
        // peuvent porter le même rang sans que cela pose de problème.
        assertThat(plan.getNumeroOdre()).isEqualTo("3");
    }

    @Test
    @DisplayName("Une correction partielle n'efface pas ce qu'un autre écran a saisi")
    void correctionPartielle_nEffaceRien() {
        plan.setWorkflowId(UUID.randomUUID());
        plan.setStatus(StatutEnum.NON_TRAITER);
        plan.setCauseIdentifiees("Procédure absente");
        plan.setSolutionRetenues("Procédure rédigée et diffusée");

        PlanActionDto dto = new PlanActionDto();
        dto.setId(plan.getId());
        dto.setObservation("Menée avec l'équipe terrain");

        service.corriger(dto);

        // Chaque écran ne saisit qu'une partie de l'action, et recopier le DTO champ par champ
        // faisait effacer par l'un ce que l'autre venait d'écrire.
        assertThat(plan.getObservation()).isEqualTo("Menée avec l'équipe terrain");
        assertThat(plan.getCauseIdentifiees()).isEqualTo("Procédure absente");
        assertThat(plan.getSolutionRetenues()).isEqualTo("Procédure rédigée et diffusée");
    }

    @Test
    @DisplayName("Une chaîne vide efface, elle : c'est une saisie, pas une omission")
    void chaineVide_efface() {
        plan.setWorkflowId(UUID.randomUUID());
        plan.setStatus(StatutEnum.NON_TRAITER);
        plan.setObservation("À revoir");

        PlanActionDto dto = new PlanActionDto();
        dto.setId(plan.getId());
        dto.setObservation("");

        service.corriger(dto);

        assertThat(plan.getObservation()).isEmpty();
    }

    @Test
    @DisplayName("Le constat d'efficacité du circuit est consigné sur l'action")
    void constatEfficacite_consigne() {
        service.updateWorkflowState(plan.getId(), "Soldée", "TRAITER",
                Map.of("constatEfficacite", "Aucune récidive sur deux trimestres"));

        // Le critère était fixé à la définition de l'action et n'était confronté à rien : on savait
        // ce qu'on attendait, jamais ce qu'on avait obtenu.
        assertThat(plan.getConstatEfficacite()).isEqualTo("Aucune récidive sur deux trimestres");
    }

    @Test
    @DisplayName("Un plan engagé ne se supprime plus depuis la fiche")
    void planEngage_suppressionRefusee() {
        plan.setWorkflowId(UUID.randomUUID());

        assertThatThrownBy(() -> service.delete(plan.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("engagé");

        verify(planActionRepository, never()).delete(any(PlanAction.class));
    }

    @Test
    @DisplayName("Un plan pas encore confié se supprime, et la clôture est réévaluée")
    void planNonEngage_supprime() {
        service.delete(plan.getId());

        verify(planActionRepository).delete(plan);
        // Une action de moins à mener : les conditions posées sur le dossier — actions toutes
        // affectées, actions toutes soldées — peuvent devenir vraies, et rien d'autre ne le dira.
        verify(plansActionService).actualiserLesFaits(dossier);
    }

    @Test
    @DisplayName("Une ré-attribution par le circuit met à jour le nom, pas seulement l'identifiant")
    void reattribution_nommeLeNouveauResponsable() {
        UUID nouveau = UUID.randomUUID();
        when(utilisateurClient.getUserById(anyString()))
                .thenReturn(Map.of("data", Map.of("email", "ada@exemple.fr", "nomComplet", "Ada Lovelace")));

        service.updateWorkflowState(plan.getId(), "À Traiter", "NON_TRAITER",
                Map.of("responsableId", nouveau.toString()));

        // Le nouvel identifiant sous l'ancien nom est le pire des deux : plus rien ne dit qui
        // répond de l'action, et les relances partent à la mauvaise adresse.
        assertThat(plan.getResponsableId()).isEqualTo(nouveau);
        assertThat(plan.getResponsableNomComplet()).isEqualTo("Ada Lovelace");
        assertThat(plan.getResponsableEmail()).isEqualTo("ada@exemple.fr");
    }

    @Test
    @DisplayName("Un annuaire indisponible n'annule pas le transfert mais efface le nom devenu faux")
    void annuaireIndisponible_nomEfface() {
        UUID nouveau = UUID.randomUUID();
        when(utilisateurClient.getUserById(anyString())).thenThrow(new RuntimeException("user-service injoignable"));

        service.updateWorkflowState(plan.getId(), "À Traiter", "NON_TRAITER",
                Map.of("responsableId", nouveau.toString()));

        assertThat(plan.getResponsableId()).isEqualTo(nouveau);
        assertThat(plan.getResponsableNomComplet()).isNull();
    }
}
