package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.NonConformiteServiceImpl;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.service.SendMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Les plans d'action tels que la fiche de non-conformité les montre.
 *
 * <p>Un plan est rattaché à son dossier de deux façons : par la colonne {@code non_conforme_id},
 * sur laquelle travaillent tous les services, et par la collection {@code NonConformite.planActions},
 * qui alimente seule la fiche. Un plan créé par son propre service n'écrivait que la première : il
 * existait, son circuit le pilotait, la clôture du dossier attendait qu'il soit soldé — et la fiche
 * ne le montrait nulle part. On demandait donc au pilote et au responsable qualité de valider des
 * actions qu'aucun écran ne leur présentait.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlansVisiblesSurLaFicheTest {

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private NonConformiteMapper nonConformiteMapper;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private ReferentielClient referentielClient;
    @Mock private PieceJointeStockageService fichierService;
    @Mock private NonConformiteFichierService ncFichierService;
    @Mock private SendMailService sendMailService;
    @Mock private PlanActionRepository planActionRepository;
    @Mock private WorkflowClient workflowClient;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;
    @Mock private PermissionChecker permissionChecker;

    @InjectMocks private NonConformiteServiceImpl service;

    private final UUID dossier = UUID.randomUUID();
    private NonConformiteDto fiche;

    @BeforeEach
    void dossierExistant() {
        fiche = new NonConformiteDto();
        fiche.setId(dossier);

        when(nonConformiteRepository.existsById(dossier)).thenReturn(true);
        when(nonConformiteRepository.getReferenceById(dossier)).thenReturn(new NonConformite());
        when(nonConformiteMapper.toDto(any(NonConformite.class))).thenReturn(fiche);
        when(fichierService.getPjByEntityId(any())).thenReturn(List.of());
    }

    private PlanAction plan(UUID id, String action) {
        PlanAction plan = new PlanAction();
        plan.setId(id);
        plan.setNonConformeId(dossier);
        plan.setActionCorrective(action);
        return plan;
    }

    private void planEnBase(PlanAction... plans) {
        when(planActionRepository.findPlanActionsByNonConformeId(dossier)).thenReturn(List.of(plans));
        when(planActionMapper.toDto(any(PlanAction.class))).thenAnswer(invocation -> {
            PlanAction source = invocation.getArgument(0);
            PlanActionDto dto = new PlanActionDto();
            dto.setId(source.getId());
            dto.setActionCorrective(source.getActionCorrective());
            return dto;
        });
    }

    @Test
    @DisplayName("Un plan que la collection ne porte pas apparaît quand même sur la fiche")
    void planNonRattache_visible() {
        UUID planId = UUID.randomUUID();
        planEnBase(plan(planId, "Mettre à jour la procédure"));

        NonConformiteDto lue = service.getNonConformiteById(dossier);

        assertThat(lue.getPlanActions())
                .withFailMessage("Le plan existe, son circuit le pilote et la clôture l'attend : "
                        + "ne pas le montrer revient à demander de valider à l'aveugle.")
                .extracting(PlanActionDto::getId)
                .containsExactly(planId);
    }

    @Test
    @DisplayName("Un plan déjà porté n'est pas montré deux fois")
    void planDejaRattache_pasDeDoublon() {
        UUID planId = UUID.randomUUID();
        PlanActionDto deja = new PlanActionDto();
        deja.setId(planId);
        fiche.setPlanActions(new ArrayList<>(List.of(deja)));
        planEnBase(plan(planId, "Mettre à jour la procédure"));

        NonConformiteDto lue = service.getNonConformiteById(dossier);

        assertThat(lue.getPlanActions()).extracting(PlanActionDto::getId).containsExactly(planId);
    }

    @Test
    @DisplayName("Un dossier sans plan d'action n'en invente aucun")
    void aucunPlan() {
        planEnBase();

        NonConformiteDto lue = service.getNonConformiteById(dossier);

        assertThat(lue.getPlanActions()).isNullOrEmpty();
    }
}
