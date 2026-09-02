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
import com.qualiapproche.amelioration.repository.SourceNonConformiteRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.NonConformiteServiceImpl;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.dto.PieceJointeDTO;
import com.qualiapproche.common.service.SendMailService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les pièces jointes suivent la fiche à chacune de ses mises à jour.
 *
 * <p>{@code PieceJointeStockageService} sait aligner les pièces d'un dossier — déposer les
 * nouvelles, retirer du serveur d'objets et de la base celles que le client ne mentionne plus. Rien
 * ne vérifiait en revanche que les points d'entrée de mise à jour l'<b>appellent</b> : la ligne
 * pouvait disparaître de l'un d'eux sans qu'aucun test ne bronche, et un fichier ajouté depuis
 * l'écran concerné aurait été perdu sans erreur — la réponse paraissant valide.</p>
 *
 * <p>Trois points d'entrée mettent une fiche à jour, et les trois doivent le faire : la mise à jour
 * unitaire, la mise à jour groupée, et la mise à jour par le corps seul.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PiecesJointesDeLaMiseAJourTest {

    private static final UUID DOSSIER = UUID.fromString("6f1d0c4a-0000-4000-8000-0000000000f1");

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

    private NonConformite enBase;

    @BeforeEach
    void setUp() {
        enBase = new NonConformite();
        enBase.setId(DOSSIER);
        when(nonConformiteRepository.findById(DOSSIER)).thenReturn(Optional.of(enBase));
        // La lecture par identifiant passe par existsById/getReferenceById, non par findById.
        when(nonConformiteRepository.existsById(DOSSIER)).thenReturn(true);
        when(nonConformiteRepository.getReferenceById(DOSSIER)).thenReturn(enBase);
        when(nonConformiteRepository.save(any(NonConformite.class))).thenAnswer(a -> a.getArgument(0));
        when(nonConformiteMapper.toDto(any(NonConformite.class))).thenReturn(new NonConformiteDto());
    }

    /** Ce que l'écran renvoie : un fichier neuf en base64, et une pièce déjà connue. */
    private NonConformiteDto ficheAvecPieces() {
        NonConformiteDto dto = new NonConformiteDto();
        dto.setId(DOSSIER);
        dto.setFichiers(List.of(
                PieceJointeDTO.builder().nom("rapport.pdf").type("application/pdf")
                        .fichier(new byte[]{1, 2, 3}).build(),
                PieceJointeDTO.builder().nom("deja.pdf").url("non-conformite/DSI/deja.pdf").build()));
        return dto;
    }

    @SuppressWarnings("unchecked")
    private List<PieceJointeDTO> piecesTransmises() {
        ArgumentCaptor<List<PieceJointeDTO>> pieces = ArgumentCaptor.forClass(List.class);
        verify(ncFichierService).synchroniser(pieces.capture(), eq(DOSSIER));
        return pieces.getValue();
    }

    @Test
    @DisplayName("La mise à jour unitaire aligne les pièces du dossier sur ce que l'écran renvoie")
    void miseAJourUnitaire_alignelesPieces() throws IOException {
        service.updateNonConformite(DOSSIER, ficheAvecPieces());

        // La liste part telle quelle : c'est le service de stockage qui distingue le dépôt neuf de
        // la pièce conservée, et qui supprime celles qui n'y figurent plus.
        assertThat(piecesTransmises()).extracting(PieceJointeDTO::getNom)
                .containsExactly("rapport.pdf", "deja.pdf");
    }

    @Test
    @DisplayName("La mise à jour par le corps seul les aligne aussi")
    void miseAJourParLeCorps_alignelesPieces() {
        // Ce point d'entrée les ignorait : le mapper écarte la collection — à raison, elle est en
        // orphanRemoval — et rien ne prenait le relais. Un fichier ajouté depuis un écran passant
        // par là était perdu sans erreur.
        service.update(ficheAvecPieces());

        assertThat(piecesTransmises()).hasSize(2);
    }

    @Test
    @DisplayName("La mise à jour groupée les aligne dossier par dossier")
    void miseAJourGroupee_alignelesPieces() throws IOException {
        service.updateNonConformites(List.of(ficheAvecPieces()));

        assertThat(piecesTransmises()).extracting(PieceJointeDTO::getNom)
                .containsExactly("rapport.pdf", "deja.pdf");
    }

    @Test
    @DisplayName("Le nom d'origine et l'extension accompagnent la pièce jusqu'à l'écran")
    void pieceRelue_porteSonNom() {
        // Sans eux, l'écran n'a qu'une référence d'objet stocké : ni le bon libellé, ni la bonne
        // icône. Le nom est d'ailleurs exigé au dépôt — une pièce sans extension est refusée.
        when(fichierService.getPjByEntityId(DOSSIER)).thenReturn(List.of(
                PieceJointeDTO.builder().nom("rapport.pdf").ext("pdf")
                        .type("application/pdf").url("non-conformite/DSI/rapport.pdf").build()));
        NonConformiteDto rendu = new NonConformiteDto();
        rendu.setId(DOSSIER);
        when(nonConformiteMapper.toDto(any(NonConformite.class))).thenReturn(rendu);

        NonConformiteDto lue = service.getNonConformiteById(DOSSIER);

        assertThat(lue.getFichiers()).singleElement()
                .satisfies(piece -> {
                    assertThat(piece.getNom()).isEqualTo("rapport.pdf");
                    assertThat(piece.getExt()).isEqualTo("pdf");
                    assertThat(piece.getUrl()).isEqualTo("non-conformite/DSI/rapport.pdf");
                });
    }
}
