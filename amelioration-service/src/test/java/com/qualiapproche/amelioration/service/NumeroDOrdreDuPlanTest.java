package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteResumeMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlanActionServiceImpl;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.dto.PlanActionDto;
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le rang d'un plan d'action parmi ceux de sa non-conformité.
 *
 * <p>Ce n'est qu'un repère de lecture, et il reste saisissable. Mais il était calculé par l'écran à
 * partir des seuls plans qu'il avait sous les yeux : un plan créé autrement n'en recevait aucun, et
 * la colonne « N° Ordre » restait vide — l'action n'était alors désignable que par sa
 * description.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NumeroDOrdreDuPlanTest {

    @Mock private PlanActionRepository planActionRepository;
    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private NonConformiteResumeMapper nonConformiteResumeMapper;
    @Mock private PieceJointeStockageService fichierService;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;
    @Mock private WorkflowClient workflowClient;

    @InjectMocks private PlanActionServiceImpl service;

    private final UUID nonConformiteId = UUID.randomUUID();

    @BeforeEach
    void dossierExistant() {
        NonConformite nc = new NonConformite();
        nc.setId(nonConformiteId);
        when(nonConformiteRepository.findById(nonConformiteId)).thenReturn(Optional.of(nc));
        when(planActionMapper.toEntity(any(PlanActionDto.class))).thenAnswer(invocation -> new PlanAction());
        when(planActionRepository.save(any(PlanAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(planActionMapper.toDto(any(PlanAction.class))).thenReturn(new PlanActionDto());
    }

    private PlanAction planNumerote(String numero) {
        PlanAction plan = new PlanAction();
        plan.setNumeroOdre(numero);
        return plan;
    }

    private PlanAction creer() throws IOException {
        PlanActionDto dto = new PlanActionDto();
        dto.setNonConformeId(nonConformiteId);
        service.createPlanActionDto(dto);

        ArgumentCaptor<PlanAction> enregistre = ArgumentCaptor.forClass(PlanAction.class);
        verify(planActionRepository).save(enregistre.capture());
        return enregistre.getValue();
    }

    @Test
    @DisplayName("Le premier plan d'un dossier porte le rang un")
    void premierPlan() throws IOException {
        when(planActionRepository.findPlanActionsByNonConformeId(nonConformiteId)).thenReturn(List.of());

        assertThat(creer().getNumeroOdre()).isEqualTo("1");
    }

    @Test
    @DisplayName("Le rang suit le plus grand déjà attribué, et non le nombre de plans")
    void apresUneSuppression() throws IOException {
        // Trois plans ont été créés, le deuxième supprimé. Compter les lignes aurait redonné le
        // rang trois, et le plan ajouté se serait inséré au milieu de la liste.
        when(planActionRepository.findPlanActionsByNonConformeId(nonConformiteId))
                .thenReturn(List.of(planNumerote("1"), planNumerote("3")));

        assertThat(creer().getNumeroOdre()).isEqualTo("4");
    }

    @Test
    @DisplayName("Les numéros hérités du format « P-A-n » continuent la série")
    void numerotationHeritee() throws IOException {
        // Les dossiers antérieurs portent « P-A-1 », « P-A-2 » : on ne repart pas de un.
        when(planActionRepository.findPlanActionsByNonConformeId(nonConformiteId))
                .thenReturn(List.of(planNumerote("P-A-2"), planNumerote(null), planNumerote("  ")));

        assertThat(creer().getNumeroOdre()).isEqualTo("3");
    }

    @Test
    @DisplayName("Un rang fourni par la demande est conservé")
    void numeroFourni() throws IOException {
        when(planActionRepository.findPlanActionsByNonConformeId(nonConformiteId)).thenReturn(List.of());
        when(planActionMapper.toEntity(any(PlanActionDto.class))).thenAnswer(invocation -> planNumerote("9"));

        // Le rang est saisissable : la personne qui définit les actions choisit l'ordre dans
        // lequel elle veut les lire.
        assertThat(creer().getNumeroOdre()).isEqualTo("9");
    }
}
