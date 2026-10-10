package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
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
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.service.SendMailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.numerotation.NumerotationDesNonConformites;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Supprimer une non-conformité emporte ce qui n'existe que par elle : ses plans, leurs circuits,
 * ses pièces. Le moteur et les fichiers ne sont touchés qu'après le commit ; et un dossier ouvert
 * par un audit ne se supprime pas.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SuppressionDUnDossierTest {

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
    @Mock private PermissionChecker permissionChecker;
    @Mock private NumerotationDesNonConformites numerotation;
    @InjectMocks private NonConformiteServiceImpl service;

    private final UUID dossier = UUID.randomUUID();
    private final UUID plan = UUID.randomUUID();

    @BeforeEach
    void ouvrirUneTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        // L'appelant a la portée transverse : la suppression par identifiant applique désormais la
        // règle de visibilité de la fiche, que ces tests ne jugent pas.
        when(permissionChecker.detient(any(String[].class))).thenReturn(true);
    }

    @AfterEach
    void fermer() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private NonConformite nc(String cleOrigine) {
        NonConformite nc = new NonConformite();
        nc.setId(dossier);
        nc.setNumeroReference("NFQT-DQA-2026-00007");
        nc.setCleOrigine(cleOrigine);
        when(nonConformiteRepository.findById(dossier)).thenReturn(Optional.of(nc));
        PlanAction p = new PlanAction();
        p.setId(plan);
        when(planActionRepository.findPlanActionsByNonConformeId(dossier)).thenReturn(List.of(p));
        return nc;
    }

    @Test
    @DisplayName("Les plans partent avec le dossier ; moteur et fichiers seulement après le commit")
    void emporte_plans_puis_moteur_et_fichiers_apres_commit() {
        NonConformite nc = nc(null);

        service.delete(dossier);

        verify(planActionRepository).deleteAllById(List.of(plan));
        verify(nonConformiteRepository).delete(nc);
        verify(workflowClient, never()).oublierRessource(any());
        verify(fichierService, never()).supprimerTout(any());

        commit();
        verify(workflowClient).oublierRessource(dossier);
        verify(workflowClient).oublierRessource(plan);
        verify(fichierService).supprimerTout(dossier);
        verify(fichierService).supprimerTout(plan);
    }

    @Test
    @DisplayName("Un dossier ouvert par un audit ne se supprime pas")
    void dossier_d_audit_refuse() {
        nc("AUDIT-CONSTAT:1b4e28ba-2fa1-11d2-883f-0016d3cca427");

        assertThatThrownBy(() -> service.delete(dossier))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        verify(nonConformiteRepository, never()).delete(any(NonConformite.class));
        verify(planActionRepository, never()).deleteAllById(any());
    }

    @Test
    @DisplayName("Un dossier inexistant répond 404")
    void inexistant() {
        when(nonConformiteRepository.findById(dossier)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(dossier))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }
}
