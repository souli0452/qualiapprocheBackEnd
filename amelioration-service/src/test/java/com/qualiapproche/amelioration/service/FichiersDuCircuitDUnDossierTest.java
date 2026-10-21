package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les fichiers qu'un dossier reçoit du circuit : où ils se rangent, et à qui ils appartiennent.
 *
 * <p>Le rangement se lit sur le dossier, jamais sur la requête ; et une référence de fichier ne
 * vaut pas autorisation de lire celui d'un autre dossier. Une pièce déposée par le circuit
 * accompagne une décision : l'alignement des pièces de la fiche ne doit jamais l'emporter.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FichiersDuCircuitDUnDossierTest {

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private PieceJointeStockageService stockage;

    @InjectMocks private NonConformiteFichierService fichiers;

    private NonConformite dossier;

    @BeforeEach
    void preparer() {
        dossier = new NonConformite();
        dossier.setId(UUID.randomUUID());
        dossier.setStructureSoumissionLibelle("Direction générale");
        when(nonConformiteRepository.findById(dossier.getId())).thenReturn(Optional.of(dossier));
    }

    @Test
    @DisplayName("La structure responsable prime pour ranger les fichiers du dossier")
    void la_structure_responsable_prime() {
        dossier.setStructureResponsableSigle("DQA");
        dossier.setOrigineServiceLibelleCourt("GSI");

        assertThat(fichiers.dossierDeStructure(dossier)).isEqualTo("DQA");
    }

    @Test
    @DisplayName("Sans responsable, le service émetteur ; à défaut, la structure de soumission")
    void a_defaut_le_service_emetteur_puis_la_soumission() {
        dossier.setStructureResponsableSigle("  ");
        dossier.setOrigineServiceLibelleCourt("GSI");
        assertThat(fichiers.dossierDeStructure(dossier)).isEqualTo("GSI");

        dossier.setOrigineServiceLibelleCourt(null);
        assertThat(fichiers.dossierDeStructure(dossier)).isEqualTo("Direction générale");
    }

    @Test
    @DisplayName("Rien de renseigné : le stockage retombe sur son dossier de repli")
    void rien_de_renseigne_rend_nul() {
        dossier.setStructureSoumissionLibelle(null);

        assertThat(fichiers.dossierDeStructure(dossier)).isNull();
    }

    @Test
    @DisplayName("Un fichier du circuit est marqué comme tel, rangé sous la structure du dossier")
    void un_fichier_du_circuit_est_marque() {
        dossier.setStructureResponsableSigle("DQA");
        PieceJointe piece = new PieceJointe();
        piece.setNom("decision.pdf");
        piece.setUrl("amelioration/DQA/decision.pdf");
        MockMultipartFile fichier = new MockMultipartFile("fichier", "decision.pdf", "application/pdf", new byte[]{1});
        when(stockage.deposer(fichier, dossier.getId(), "DQA")).thenReturn(piece);

        String reference = fichiers.deposer(dossier.getId(), fichier);

        assertThat(reference).isEqualTo("amelioration/DQA/decision.pdf");
        assertThat(piece.isDeposeParCircuit()).isTrue();
        verify(pieceJointeRepository).save(piece);
    }

    @Test
    @DisplayName("Un dépôt sur un dossier inconnu répond 404 et n'atteint pas le stockage")
    void un_depot_sur_un_dossier_inconnu_repond_404() {
        UUID inconnu = UUID.randomUUID();

        assertThatThrownBy(() -> fichiers.deposer(inconnu, new MockMultipartFile("f", new byte[]{1})))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(stockage, never()).deposer(any(org.springframework.web.multipart.MultipartFile.class), any(), any());
    }

    @Test
    @DisplayName("La référence d'un fichier d'un autre dossier ne s'ouvre pas")
    void la_reference_d_un_autre_dossier_ne_s_ouvre_pas() {
        when(pieceJointeRepository.findByUrlAndEntityId("amelioration/DQA/preuve.pdf", dossier.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fichiers.pieceJointe(dossier.getId(), "amelioration/DQA/preuve.pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("L'alignement des pièces d'un dossier inconnu se fait sans sous-dossier de structure")
    void l_alignement_d_un_dossier_inconnu_se_fait_sans_sous_dossier() {
        UUID inconnu = UUID.randomUUID();

        fichiers.synchroniser(List.of(), inconnu);

        verify(stockage).synchroniser(eq(List.of()), eq(inconnu), isNull());
    }
}
