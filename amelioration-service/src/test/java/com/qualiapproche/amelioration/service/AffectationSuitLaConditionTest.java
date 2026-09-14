package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.ActionRepository;
import com.qualiapproche.amelioration.repository.EfficaciteRepository;
import com.qualiapproche.amelioration.repository.NiveauNonConformiteRepository;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.NonConformiteServiceImpl;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.service.SendMailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'engagement des plans d'action suit le <b>point de contrôle</b> franchi, non une étape nommée.
 *
 * <p>Il se lisait sur l'arrivée à l'état {@code VALIDATION_RS}, écrit en dur. Le circuit étant
 * paramétrable, supprimer cette étape depuis l'éditeur — ou seulement changer son état de
 * traitement — suffisait à ce que plus aucun plan ne soit jamais confié : les responsables
 * n'étaient pas prévenus, aucun circuit de plan ne s'ouvrait, et la non-conformité, dont la clôture
 * exige que tous ses plans soient soldés, ne pouvait plus jamais être close. Sans message, et sans
 * issue depuis l'interface.</p>
 *
 * <p>Ce qui déclenche l'engagement est désormais la condition que la transition franchie portait :
 * {@code PLANS_ACTION_AFFECTES}, précisément le fait qui garantit que chaque action a un
 * responsable — la seule chose dont l'affectation ait besoin.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AffectationSuitLaConditionTest {

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private NonConformiteMapper nonConformiteMapper;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private TypeNonConformiteRepository typeNonConformiteRepository;
    @Mock private ReferentielClient referentielClient;
    @Mock private EfficaciteRepository efficaciteRepository;
    @Mock private ActionRepository actionRepository;
    @Mock private NiveauNonConformiteRepository niveauNonConformiteRepository;
    @Mock private PieceJointeStockageService fichierService;
    @Mock private NonConformiteFichierService ncFichierService;
    @Mock private SendMailService sendMailService;
    @Mock private PlanActionRepository planActionRepository;
    @Mock private WorkflowClient workflowClient;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;

    @InjectMocks private NonConformiteServiceImpl service;

    private final UUID ncId = UUID.randomUUID();

    private void dossierExistant() {
        NonConformite nc = new NonConformite();
        nc.setId(ncId);
        when(nonConformiteRepository.findById(ncId)).thenReturn(Optional.of(nc));
    }

    @Test
    @DisplayName("La condition d'affectation franchie confie les plans, quelle que soit l'étape atteinte")
    void conditionFranchie_confieLesPlans() {
        dossierExistant();

        // L'étape atteinte porte un état que l'ancien déclenchement n'aurait pas reconnu.
        service.updateWorkflowState(ncId, "EN_COURS", "Contre-validation", "SUIVI_RQ", Map.of(),
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_AFFECTES);

        verify(plansActionService).confierLesPlans(ncId);
    }

    @Test
    @DisplayName("Atteindre l'étape de validation qualité sans la condition ne confie rien")
    void etapeSeule_neConfieRien() {
        dossierExistant();

        // Exactement l'ancien déclencheur : l'état VALIDATION_RS, sans point de contrôle franchi.
        service.updateWorkflowState(ncId, "EN_COURS", "Validation RS", "VALIDATION_RS", Map.of(), null);

        verify(plansActionService, never()).confierLesPlans(ncId);
    }

    @Test
    @DisplayName("Une autre condition franchie ne confie rien")
    void autreCondition_neConfieRien() {
        dossierExistant();

        service.updateWorkflowState(ncId, "EN_COURS", "Validation", "VALIDATION", Map.of(),
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_COMPLETS);

        verify(plansActionService, never()).confierLesPlans(ncId);
    }

    @Test
    @DisplayName("La casse et les espaces du fait n'empêchent pas l'engagement")
    void faitEcritAutrement_confieQuandMeme() {
        dossierExistant();

        service.updateWorkflowState(ncId, "EN_COURS", "Validation", "VALIDATION", Map.of(),
                "  plans_action_affectes  ");

        verify(plansActionService).confierLesPlans(ncId);
    }
}
