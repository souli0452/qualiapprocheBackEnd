package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.entities.mappers.PieceJointeMapper;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.common.dto.PieceJointeDTO;
import com.qualiapproche.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Alignement des pièces jointes d'un dossier sur ce que le client renvoie.
 *
 * <p>La mise à jour effaçait auparavant toutes les pièces avant de réécrire ce que le client
 * envoyait. Cela ne tenait que tant que le client recevait le contenu de chaque fichier et le
 * renvoyait tel quel ; il ne reçoit plus que des références. Ces tests fixent la règle qui remplace
 * ce comportement.</p>
 */
class PieceJointeStockageServiceTest {

    private static final UUID DOSSIER = UUID.randomUUID();
    private static final String SIGLE = "DSI";

    private StorageService storageService;
    private PieceJointeRepository pieceJointeRepository;
    private PieceJointeStockageService service;

    @BeforeEach
    void setUp() throws Exception {
        storageService = mock(StorageService.class);
        pieceJointeRepository = mock(PieceJointeRepository.class);
        PieceJointeMapper mapper = mock(PieceJointeMapper.class);
        when(pieceJointeRepository.save(any(PieceJointe.class))).thenAnswer(i -> i.getArgument(0));
        when(storageService.uploadContent(any(), any(), any(), anyString(), any()))
                .thenReturn("NON_CONFORMITE/DSI/nouvelle.pdf");

        service = new PieceJointeStockageService(storageService, pieceJointeRepository, mapper);
    }

    private PieceJointe existante(String reference, String nom) {
        return PieceJointe.builder().id(UUID.randomUUID()).entityId(DOSSIER).url(reference).nom(nom).build();
    }

    private PieceJointeDTO reference(String reference) {
        PieceJointeDTO dto = new PieceJointeDTO();
        dto.setUrl(reference);
        return dto;
    }

    private PieceJointeDTO nouveau(String nom) {
        PieceJointeDTO dto = new PieceJointeDTO();
        dto.setNom(nom);
        dto.setType("application/pdf");
        dto.setFichier("contenu".getBytes());
        return dto;
    }

    @Test
    @DisplayName("Une pièce que le client renvoie par sa seule référence est conservée")
    void pieceReferencee_conservee() {
        PieceJointe deja = existante("NON_CONFORMITE/DSI/deja.pdf", "deja.pdf");
        when(pieceJointeRepository.findAllByEntityIdAndDeposeParCircuitFalse(DOSSIER))
                .thenReturn(List.of(deja));

        service.synchroniser(List.of(reference("NON_CONFORMITE/DSI/deja.pdf")), DOSSIER, SIGLE);

        // Sans cette règle, la pièce était détruite puis réécrite à partir d'un contenu que le
        // client n'a plus : il n'en serait resté qu'une ligne sans fichier.
        verify(pieceJointeRepository, never()).deleteAll(any());
        verify(pieceJointeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une pièce que le client ne mentionne plus est supprimée, contenu compris")
    void pieceRetiree_supprimee() throws Exception {
        PieceJointe gardee = existante("NON_CONFORMITE/DSI/gardee.pdf", "gardee.pdf");
        PieceJointe retiree = existante("NON_CONFORMITE/DSI/retiree.pdf", "retiree.pdf");
        when(pieceJointeRepository.findAllByEntityIdAndDeposeParCircuitFalse(DOSSIER))
                .thenReturn(List.of(gardee, retiree));

        service.synchroniser(List.of(reference("NON_CONFORMITE/DSI/gardee.pdf")), DOSSIER, SIGLE);

        verify(storageService).deleteFile("NON_CONFORMITE/DSI/retiree.pdf");
        verify(pieceJointeRepository).deleteAll(List.of(retiree));
    }

    @Test
    @DisplayName("Une liste vide ne supprime rien : c'est un écran muet, non un retrait")
    void listeVide_neSupprimeRien() {
        PieceJointe existante = existante("NON_CONFORMITE/DSI/gardee.pdf", "gardee.pdf");
        when(pieceJointeRepository.findAllByEntityIdAndDeposeParCircuitFalse(DOSSIER))
                .thenReturn(List.of(existante));

        service.synchroniser(List.of(), DOSSIER, SIGLE);

        // Les écrans qui n'affichent pas les pièces jointes envoient une liste vide selon leur
        // formulaire : enregistrer un dossier depuis l'un d'eux supprimait toutes ses pièces.
        // Retirer la dernière pièce passe par sa suppression, qui dit ce qu'elle fait.
        verify(pieceJointeRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Une entrée porteuse d'un contenu est déposée sous le dossier du module")
    void entreeAvecContenu_deposee() throws Exception {
        when(pieceJointeRepository.findAllByEntityIdAndDeposeParCircuitFalse(DOSSIER)).thenReturn(List.of());

        service.synchroniser(List.of(nouveau("rapport.pdf")), DOSSIER, SIGLE);

        verify(storageService).uploadContent(any(), eq("rapport.pdf"), eq("application/pdf"),
                eq("non-conformite"), eq(SIGLE));

        ArgumentCaptor<PieceJointe> enregistree = ArgumentCaptor.forClass(PieceJointe.class);
        verify(pieceJointeRepository).save(enregistree.capture());
        assertThat(enregistree.getValue().getUrl()).isEqualTo("NON_CONFORMITE/DSI/nouvelle.pdf");
        assertThat(enregistree.getValue().getEntityId()).isEqualTo(DOSSIER);
        assertThat(enregistree.getValue().getExt()).isEqualTo("pdf");
    }

    @Test
    @DisplayName("Une liste absente ne touche à rien : le client n'a pas parlé des pièces")
    void listeAbsente_sansEffet() {
        service.synchroniser(null, DOSSIER, SIGLE);

        verify(pieceJointeRepository, never()).findAllByEntityIdAndDeposeParCircuitFalse(any());
        verify(pieceJointeRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Le contenu d'une pièce n'est jamais rendu avec la liste")
    void listeSansContenu() {
        PieceJointeMapper mapper = mock(PieceJointeMapper.class);
        PieceJointe piece = existante("NON_CONFORMITE/DSI/deja.pdf", "deja.pdf");
        PieceJointeDTO dto = new PieceJointeDTO();
        dto.setNom("deja.pdf");
        dto.setUrl("NON_CONFORMITE/DSI/deja.pdf");
        when(mapper.toDto(piece)).thenReturn(dto);
        when(pieceJointeRepository.findAllByEntityIdAndDeposeParCircuitFalse(DOSSIER))
                .thenReturn(List.of(piece));

        List<PieceJointeDTO> pieces =
                new PieceJointeStockageService(storageService, pieceJointeRepository, mapper)
                        .getPjByEntityId(DOSSIER);

        // Ce chargement s'exécute sur chaque ligne de chaque page de liste : y joindre le fichier
        // ferait un aller-retour vers le serveur d'objets par pièce et par ligne.
        assertThat(pieces).singleElement().satisfies(p -> {
            assertThat(p.getFichier()).isNull();
            assertThat(p.getUrl()).isEqualTo("NON_CONFORMITE/DSI/deja.pdf");
        });
    }
}
