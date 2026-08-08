package com.qualiapproche.support.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.storage.StorageService;
import com.qualiapproche.support.model.PieceJointeEtape;
import com.qualiapproche.support.repository.PieceJointeEtapeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pièces réclamées par une étape de circuit sur un document ou une demande.
 *
 * <p>Le rangement fait l'autorisation : la pièce est déposée sous un chemin qui contient
 * l'identifiant du dossier, et le téléchargement n'accepte qu'une référence portant ce chemin. Sans
 * cette vérification, le point d'entrée rendrait <b>n'importe quel objet</b> du serveur de fichiers à
 * qui sait lire un document — il suffirait d'en deviner la clé.</p>
 */
class FichiersDEtapeServiceTest {

    private static final UUID DOSSIER = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID AUTRE_DOSSIER = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private StorageService stockage;
    private PieceJointeEtapeRepository pieces;
    private FichiersDEtapeService service;

    @BeforeEach
    void setUp() {
        stockage = mock(StorageService.class);
        pieces = mock(PieceJointeEtapeRepository.class);
        service = new FichiersDEtapeService(stockage, pieces);
        when(pieces.save(any(PieceJointeEtape.class))).thenAnswer(i -> i.getArgument(0));
        when(pieces.findByReference(any())).thenReturn(Optional.empty());
    }

    /** La ligne de correspondance telle que le dépôt l'inscrit. */
    private PieceJointeEtape ligne(String famille, UUID dossier, String reference, String nom) {
        return PieceJointeEtape.builder()
                .famille(famille).dossierId(dossier).reference(reference).nom(nom)
                .type("application/pdf").taille(7).build();
    }

    private MultipartFile fichier() {
        return new MockMultipartFile("file", "avis.pdf", "application/pdf", "contenu".getBytes());
    }

    // ------------------------------------------------------------------ dépôt

    @Test
    @DisplayName("La pièce est rangée sous le dossier concerné, et sa référence est rendue")
    void depot_rangeSousLeDossier() throws Exception {
        when(stockage.uploadFile(any(), eq("pieces-etape"), eq("documents"), eq(DOSSIER.toString())))
                .thenReturn("pieces-etape/documents/" + DOSSIER + "/abc.pdf");

        String reference = service.deposer("documents", DOSSIER, fichier());

        // L'identifiant du dossier figure dans la référence : c'est lui qui autorisera la relecture.
        assertThat(reference).isEqualTo("pieces-etape/documents/" + DOSSIER + "/abc.pdf");
    }

    @Test
    @DisplayName("Un fichier vide est refusé plutôt que déposé")
    void fichierVide_refuse() throws Exception {
        MultipartFile vide = new MockMultipartFile("file", "vide.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.deposer("documents", DOSSIER, vide))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(stockage, never()).uploadFile(any(), any());
    }

    @Test
    @DisplayName("Serveur de fichiers indisponible : l'échec est dit, la décision ne paraît pas prise")
    void stockageIndisponible_echecExplicite() throws Exception {
        when(stockage.uploadFile(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("connexion refusée"));

        assertThatThrownBy(() -> service.deposer("documents", DOSSIER, fichier()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("connexion refusée")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    // ------------------------------------------------------------------ relecture

    @Test
    @DisplayName("Une pièce du dossier se télécharge")
    void pieceDuDossier_telechargeable() throws Exception {
        String reference = "pieces-etape/documents/" + DOSSIER + "/abc.pdf";
        when(stockage.downloadFile(reference)).thenReturn(new ByteArrayInputStream("x".getBytes()));

        assertThat(service.contenu("documents", DOSSIER, reference)).isNotNull();
    }

    @Test
    @DisplayName("La pièce d'un autre dossier est refusée en 403")
    void pieceDUnAutreDossier_refusee() throws Exception {
        String reference = "pieces-etape/documents/" + AUTRE_DOSSIER + "/abc.pdf";

        assertThatThrownBy(() -> service.contenu("documents", DOSSIER, reference))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(stockage, never()).downloadFile(any());
    }

    @Test
    @DisplayName("Une référence forgée hors du rangement des pièces d'étape est refusée")
    void referenceForgee_refusee() throws Exception {
        // Le contenu d'un document publié, une sauvegarde, n'importe quel objet du dépôt.
        assertThatThrownBy(() -> service.contenu("documents", DOSSIER, "documents/PRO/secret.pdf"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.contenu("documents", DOSSIER, null))
                .isInstanceOf(BusinessException.class);

        verify(stockage, never()).downloadFile(any());
    }

    @Test
    @DisplayName("La famille compte aussi : la pièce d'une demande n'est pas celle d'un document")
    void familleDistincte_refusee() throws Exception {
        String piecedeDemande = "pieces-etape/demandes/" + DOSSIER + "/abc.pdf";

        // Un identifiant de dossier peut être connu ; la famille ne doit pas pouvoir être substituée.
        assertThatThrownBy(() -> service.contenu("documents", DOSSIER, piecedeDemande))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------ nom d'origine

    @Test
    @DisplayName("Le nom du dépôt est conservé, et proposé au téléchargement")
    void nomDOrigine_conserve() throws Exception {
        String reference = "pieces-etape/documents/" + DOSSIER + "/abc.pdf";
        when(stockage.uploadFile(any(), any(), any(), any())).thenReturn(reference);

        service.deposer("documents", DOSSIER,
                new MockMultipartFile("file", "attestation-formation-2026.pdf",
                        "application/pdf", "contenu".getBytes()));

        ArgumentCaptor<PieceJointeEtape> aInscrite = ArgumentCaptor.forClass(PieceJointeEtape.class);
        verify(pieces).save(aInscrite.capture());
        // Sans cette correspondance, l'audit recevait « abc.pdf » : la clé de l'objet ne reprend que
        // l'extension.
        assertThat(aInscrite.getValue().getNom()).isEqualTo("attestation-formation-2026.pdf");
        assertThat(aInscrite.getValue().getType()).isEqualTo("application/pdf");
        assertThat(aInscrite.getValue().getDossierId()).isEqualTo(DOSSIER);

        when(pieces.findByReference(reference))
                .thenReturn(Optional.of(ligne("documents", DOSSIER, reference,
                        "attestation-formation-2026.pdf")));
        assertThat(service.nomPropose(reference)).isEqualTo("attestation-formation-2026.pdf");
        assertThat(service.typeDeContenu(reference)).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Un chemin transmis par le navigateur est ramené au seul nom de fichier")
    void nomAvecChemin_reduit() throws Exception {
        when(stockage.uploadFile(any(), any(), any(), any())).thenReturn("pieces-etape/documents/x/a.pdf");

        service.deposer("documents", DOSSIER, new MockMultipartFile("file",
                "C:\\Users\\awa\\Bureau\\avis.pdf", "application/pdf", "x".getBytes()));

        ArgumentCaptor<PieceJointeEtape> aInscrite = ArgumentCaptor.forClass(PieceJointeEtape.class);
        verify(pieces).save(aInscrite.capture());
        // Un chemin dans un en-tête Content-Disposition n'aurait pas été exploitable.
        assertThat(aInscrite.getValue().getNom()).isEqualTo("avis.pdf");
    }

    @Test
    @DisplayName("Sans ligne de correspondance, le nom retombe sur la clé de l'objet")
    void sansLigne_nomDeLObjet() {
        // Pièces déposées avant l'existence de la table : elles restent téléchargeables.
        assertThat(service.nomPropose("pieces-etape/documents/" + DOSSIER + "/abc.pdf"))
                .isEqualTo("abc.pdf");
        assertThat(service.typeDeContenu("pieces-etape/documents/" + DOSSIER + "/abc.pdf")).isNull();
    }

    @Test
    @DisplayName("La table fait autorité : une pièce inscrite sur un autre dossier est refusée")
    void tableFaitAutorite() {
        // Même si la clé, elle, porte le bon chemin — cas d'une référence recopiée à la main.
        String reference = "pieces-etape/documents/" + DOSSIER + "/abc.pdf";
        when(pieces.findByReference(reference))
                .thenReturn(Optional.of(ligne("documents", AUTRE_DOSSIER, reference, "avis.pdf")));

        assertThatThrownBy(() -> service.contenu("documents", DOSSIER, reference))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------ bornes du dépôt

    @Test
    @DisplayName("Un exécutable est refusé avant tout rangement")
    void executable_refuse() throws Exception {
        MultipartFile executable = new MockMultipartFile("file", "outil.exe",
                "application/octet-stream", "MZ".getBytes());

        assertThatThrownBy(() -> service.deposer("documents", DOSSIER, executable))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("n'est pas admis")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Rien ne part vers le serveur de fichiers, rien ne s'inscrit en table.
        verify(stockage, never()).uploadFile(any(), any());
        verify(pieces, never()).save(any());
    }

    @Test
    @DisplayName("Une pièce trop lourde est refusée avec la limite dans le message")
    void tropLourde_refusee() throws Exception {
        MultipartFile lourde = new MockMultipartFile("file", "rapport.pdf", "application/pdf",
                new byte[26 * 1024 * 1024]);

        assertThatThrownBy(() -> service.deposer("documents", DOSSIER, lourde))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("25");
    }
}
