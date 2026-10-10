package com.qualiapproche.amelioration.controller;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.NonConformiteService;
import com.qualiapproche.amelioration.service.impl.AccesAuxDossiers;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlanActionFichierService;
import com.qualiapproche.common.config.PermissionChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Les fichiers d'un dossier suivent la règle de visibilité de sa fiche.
 *
 * <p>Le dépôt, le téléchargement et la suppression d'une pièce ne demandaient que la permission
 * générale du module : la pièce d'un dossier d'une autre structure se lisait, et surtout se
 * détruisait, sur la seule foi de son identifiant. Ces tests fixent la borne — un dossier que
 * l'appelant ne peut pas consulter répond 404, et rien n'atteint le stockage.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PiecesJointesBorneesAuDossierTest {

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PlanActionRepository planActionRepository;
    @Mock private NonConformiteService nonConformiteService;
    @Mock private PermissionChecker permissionChecker;
    @Mock private NonConformiteFichierService ncFichierService;
    @Mock private PlanActionFichierService planFichierService;
    @Mock private PieceJointeStockageService stockage;

    private NonConformiteFichierController fichiersNc;
    private PieceJointeController pieces;
    private PlanActionFichierController fichiersPlan;

    /** Un dossier de DQA, que l'agent de GSI connecté ne peut pas consulter. */
    private final NonConformite dossierDeDqa = new NonConformite();
    /** Un dossier de sa propre structure. */
    private final NonConformite dossierDeGsi = new NonConformite();
    private final PlanAction planDeDqa = new PlanAction();

    @BeforeEach
    void setUp() {
        AccesAuxDossiers acces = new AccesAuxDossiers(
                nonConformiteRepository, planActionRepository, nonConformiteService, permissionChecker);
        fichiersNc = new NonConformiteFichierController(ncFichierService, stockage, acces);
        pieces = new PieceJointeController(stockage, acces);
        fichiersPlan = new PlanActionFichierController(planFichierService, acces);

        dossierDeDqa.setId(UUID.randomUUID());
        dossierDeDqa.setStructureSoumissionId("DQA");
        dossierDeGsi.setId(UUID.randomUUID());
        dossierDeGsi.setStructureSoumissionId("GSI");
        planDeDqa.setId(UUID.randomUUID());
        planDeDqa.setNonConformeId(dossierDeDqa.getId());

        when(nonConformiteRepository.findById(dossierDeDqa.getId())).thenReturn(Optional.of(dossierDeDqa));
        when(nonConformiteRepository.findById(dossierDeGsi.getId())).thenReturn(Optional.of(dossierDeGsi));
        when(planActionRepository.findById(planDeDqa.getId())).thenReturn(Optional.of(planDeDqa));
        when(nonConformiteService.peutConsulter(dossierDeDqa)).thenReturn(false);
        when(nonConformiteService.peutConsulter(dossierDeGsi)).thenReturn(true);
        when(permissionChecker.detient(any(String[].class))).thenReturn(false);
    }

    private PieceJointe pieceDe(UUID dossier) {
        PieceJointe piece = new PieceJointe();
        piece.setId(UUID.randomUUID());
        piece.setEntityId(dossier);
        piece.setNom("preuve.pdf");
        piece.setType("application/pdf");
        when(stockage.piece(piece.getId())).thenReturn(piece);
        return piece;
    }

    private static void introuvable(org.assertj.core.api.ThrowableAssert.ThrowingCallable appel) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    @DisplayName("Le téléchargement d'une pièce d'un dossier d'une autre structure est introuvable")
    void telechargement_d_une_autre_structure_refuse() {
        PieceJointe piece = pieceDe(dossierDeDqa.getId());

        introuvable(() -> pieces.telecharger(piece.getId()));
        introuvable(() -> fichiersNc.telecharger(dossierDeDqa.getId(), "ref"));

        verify(stockage, never()).contenu(any());
        verify(ncFichierService, never()).pieceJointe(any(), anyString());
    }

    @Test
    @DisplayName("Le dépôt sur un dossier d'une autre structure, ou sur l'un de ses plans, est introuvable")
    void depot_sur_une_autre_structure_refuse() {
        MockMultipartFile fichier = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[]{1});

        introuvable(() -> fichiersNc.deposer(dossierDeDqa.getId(), fichier));
        introuvable(() -> fichiersPlan.deposer(planDeDqa.getId(), fichier));
        introuvable(() -> fichiersPlan.pieces(planDeDqa.getId()));

        verifyNoInteractions(ncFichierService, planFichierService);
    }

    @Test
    @DisplayName("La suppression d'une pièce d'un dossier d'une autre structure est refusée, la preuve reste")
    void suppression_d_une_autre_structure_refusee() {
        PieceJointe piece = pieceDe(dossierDeDqa.getId());
        PieceJointe pieceDuPlan = pieceDe(planDeDqa.getId());

        introuvable(() -> pieces.supprimer(piece.getId()));
        introuvable(() -> pieces.supprimer(pieceDuPlan.getId()));

        verify(stockage, never()).deleteById(any());
    }

    @Test
    @DisplayName("Une pièce rattachée à aucun dossier connu reste introuvable")
    void piece_orpheline_introuvable() {
        PieceJointe piece = pieceDe(UUID.randomUUID());

        introuvable(() -> pieces.telecharger(piece.getId()));
        verify(stockage, never()).contenu(any());
    }

    @Test
    @DisplayName("Les pièces d'un dossier de sa structure se lisent, se déposent et se suppriment comme avant")
    void dossier_visible_inchange() {
        PieceJointe piece = pieceDe(dossierDeGsi.getId());
        when(stockage.contenu(piece)).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(ncFichierService.deposer(any(), any())).thenReturn("ref");

        assertThat(pieces.telecharger(piece.getId()).getStatusCode().value()).isEqualTo(200);
        assertThat(fichiersNc.deposer(dossierDeGsi.getId(),
                new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[]{1})).getStatusCode().value())
                .isEqualTo(200);
        pieces.supprimer(piece.getId());

        verify(stockage).deleteById(piece.getId());
    }
}
