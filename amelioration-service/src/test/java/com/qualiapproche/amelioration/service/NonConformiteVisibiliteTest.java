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
import com.qualiapproche.amelioration.repository.SourceNonConformiteRepository;
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

/**
 * Qui voit quoi dans les listes générales de non-conformités.
 *
 * <p>Le point d'entrée « toutes les non-conformités » rendait la table entière : la restriction
 * n'existait que dans le choix des écrans, si bien qu'un agent obtenait les dossiers de toutes les
 * structures en demandant simplement la liste. Rien ne le signalait — la réponse était valide.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonConformiteVisibiliteTest {

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private NonConformiteMapper nonConformiteMapper;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private SourceNonConformiteRepository sourceNonConformiteRepository;
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

    @InjectMocks private NonConformiteServiceImpl service;

    private final Pageable page = PageRequest.of(0, 10);

    @Test
    @DisplayName("Un utilisateur ordinaire ne reçoit que ce qui le concerne")
    void utilisateurOrdinaire_visibiliteBornee() {
        when(permissionChecker.detient(any(String[].class))).thenReturn(false);
        when(nonConformiteRepository.findVisiblesPar(any(), any(), any())).thenReturn(Page.empty(page));

        service.allNonConformites(page);

        // La borne est posée par la requête, non par l'écran : c'est le seul endroit qui la
        // garantisse quel que soit l'appelant.
        verify(nonConformiteRepository).findVisiblesPar(any(), any(), any());
        verify(nonConformiteRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("L'administration et la responsabilité qualité voient tous les dossiers")
    void porteeGlobale_voitTout() {
        when(permissionChecker.detient(any(String[].class))).thenReturn(true);
        when(nonConformiteRepository.findAll(any(Pageable.class))).thenReturn(Page.empty(page));

        service.allNonConformites(page);

        // Leur fonction est transverse : la borne de structure n'a pas de sens pour elles. Voir,
        // et non décider — l'habilitation des étapes leur reste opposable.
        verify(nonConformiteRepository).findAll(any(Pageable.class));
        verify(nonConformiteRepository, never()).findVisiblesPar(anyString(), anyString(), any());
    }
}
