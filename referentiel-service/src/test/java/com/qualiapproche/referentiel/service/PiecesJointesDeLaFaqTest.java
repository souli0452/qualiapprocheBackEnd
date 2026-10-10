package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.FichierFaqDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.Faq;
import com.qualiapproche.referentiel.entities.FichierFaq;
import com.qualiapproche.referentiel.repository.FaqRepository;
import com.qualiapproche.referentiel.repository.FichierFaqRepository;
import com.qualiapproche.referentiel.service.impl.FichierFaqServiceImpl;
import com.qualiapproche.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pièces jointes de la FAQ : bornées à l'organisation de l'appelant, soumises aux règles de toute
 * pièce jointe, et tolérantes à l'absence d'un serveur de fichiers.
 */
class PiecesJointesDeLaFaqTest {

    private static final UUID MA_DIRECTION = UUID.randomUUID();
    private static final UUID AUTRE_DIRECTION = UUID.randomUUID();

    private final FichierFaqRepository fichiers = mock(FichierFaqRepository.class);
    private final FaqRepository entrees = mock(FaqRepository.class);
    private final StorageService stockage = mock(StorageService.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<StorageService> fournisseur = mock(ObjectProvider.class);
    private FichierFaqServiceImpl service;

    private Faq mienne;
    private Faq etrangere;

    @BeforeEach
    void preparer() {
        service = new FichierFaqServiceImpl(fichiers, entrees, fournisseur);
        when(fournisseur.getIfAvailable()).thenReturn(stockage);
        when(fichiers.save(any(FichierFaq.class))).thenAnswer(appel -> {
            FichierFaq f = appel.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        mienne = entree(MA_DIRECTION);
        etrangere = entree(AUTRE_DIRECTION);
        connecter(MA_DIRECTION);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private Faq entree(UUID direction) {
        Faq f = new Faq();
        f.setId(UUID.randomUUID());
        f.setDirectionId(direction);
        when(entrees.findById(f.getId())).thenReturn(Optional.of(f));
        return f;
    }

    private static void connecter(UUID direction) {
        Jwt.Builder b = Jwt.withTokenValue("jeton").header("alg", "RS256").subject("u1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (direction != null) {
            b.claim("direction_id", direction.toString());
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(b.build(), List.of()));
    }

    private FichierFaq piece(Faq entree) {
        FichierFaq f = FichierFaq.builder().faqId(entree.getId()).nom("procedure.pdf").ext("pdf")
                .type("application/pdf").url("faq/" + entree.getId() + "/procedure.pdf").build();
        f.setId(UUID.randomUUID());
        when(fichiers.findById(f.getId())).thenReturn(Optional.of(f));
        return f;
    }

    private static MockMultipartFile fichier(String nom) {
        return new MockMultipartFile("fichiers", nom, "application/pdf", "contenu".getBytes());
    }

    @Test
    @DisplayName("Déposer : la pièce est rangée sous l'entrée, extension en minuscules, et décrite sans son adresse")
    void depot() throws Exception {
        when(stockage.uploadFile(any(), anyString(), anyString())).thenReturn("faq/ref-1");
        List<MultipartFile> lot = new ArrayList<>();
        lot.add(fichier("Procédure.PDF"));
        lot.add(null);
        lot.add(new MockMultipartFile("fichiers", "vide.pdf", "application/pdf", new byte[0]));

        List<FichierFaqDto> posees = service.deposer(mienne.getId(), lot);

        assertThat(posees).singleElement().satisfies(d -> {
            assertThat(d.getNom()).isEqualTo("Procédure.PDF");
            assertThat(d.getExt()).isEqualTo("pdf");
            assertThat(d.getFaqId()).isEqualTo(mienne.getId());
        });
        ArgumentCaptor<FichierFaq> enregistree = ArgumentCaptor.forClass(FichierFaq.class);
        verify(fichiers).save(enregistree.capture());
        assertThat(enregistree.getValue().getUrl()).isEqualTo("faq/ref-1");
        verify(stockage).uploadFile(any(), org.mockito.ArgumentMatchers.eq("faq"),
                org.mockito.ArgumentMatchers.eq(mienne.getId().toString()));
    }

    @Test
    @DisplayName("Déposer sous l'entrée d'une autre organisation : introuvable, rien n'est envoyé")
    void depotChezAutrui() {
        assertThatThrownBy(() -> service.deposer(etrangere.getId(), List.of(fichier("a.pdf"))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(stockage);
    }

    @Test
    @DisplayName("Sans direction dans le jeton, aucune entrée n'est la sienne")
    void sansDirection() {
        connecter(null);

        assertThatThrownBy(() -> service.deposer(mienne.getId(), List.of(fichier("a.pdf"))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Une extension hors de la liste blanche est refusée avant tout envoi")
    void extensionRefusee() {
        assertThatThrownBy(() -> service.deposer(mienne.getId(), List.of(fichier("script.exe"))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(stockage);
        verify(fichiers, never()).save(any());
    }

    @Test
    @DisplayName("Sans serveur de fichiers : 503 qui dit ce qui manque, et la FAQ reste lisible")
    void sansServeurDeFichiers() {
        when(fournisseur.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.deposer(mienne.getId(), List.of(fichier("a.pdf"))))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getMessage()).contains("serveur de fichiers");
                });
        assertThat(service.desEntrees(List.of())).isEmpty();
        assertThat(service.desEntrees(null)).isEmpty();
    }

    @Test
    @DisplayName("Un envoi qui échoue : 502 nommant la pièce, rien n'est enregistré")
    void envoiEnEchec() throws Exception {
        when(stockage.uploadFile(any(), anyString(), anyString())).thenThrow(new RuntimeException("MinIO"));

        assertThatThrownBy(() -> service.deposer(mienne.getId(), List.of(fichier("a.pdf"))))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getMessage()).contains("a.pdf");
                });
        verify(fichiers, never()).save(any());
    }

    @Test
    @DisplayName("Lire une pièce : la sienne se lit, celle d'une autre organisation est introuvable")
    void lecture() throws Exception {
        FichierFaq miennePiece = piece(mienne);
        FichierFaq autrePiece = piece(etrangere);
        InputStream flux = new ByteArrayInputStream(new byte[]{1});
        when(stockage.downloadFile(miennePiece.getUrl())).thenReturn(flux);

        assertThat(service.contenu(miennePiece.getId())).isSameAs(flux);
        assertThat(service.decrire(miennePiece.getId()).getNom()).isEqualTo("procedure.pdf");
        assertThatThrownBy(() -> service.contenu(autrePiece.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(stockage, never()).downloadFile(autrePiece.getUrl());
    }

    @Test
    @DisplayName("Une pièce disparue du serveur de fichiers : 502, et non une erreur interne")
    void pieceDisparue() throws Exception {
        FichierFaq p = piece(mienne);
        when(stockage.downloadFile(p.getUrl())).thenThrow(new RuntimeException("NoSuchKey"));

        assertThatThrownBy(() -> service.contenu(p.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    @DisplayName("Supprimer : la ligne d'abord, l'objet ensuite ; un échec du serveur ne la ressuscite pas")
    void suppression() throws Exception {
        FichierFaq p = piece(mienne);
        doThrow(new RuntimeException("MinIO")).when(stockage).deleteFile(p.getUrl());

        assertThatCode(() -> service.supprimer(p.getId())).doesNotThrowAnyException();

        var ordre = inOrder(fichiers, stockage);
        ordre.verify(fichiers).delete(p);
        ordre.verify(stockage).deleteFile(p.getUrl());
    }

    @Test
    @DisplayName("On ne supprime pas la pièce d'une autre organisation")
    void suppressionChezAutrui() {
        FichierFaq p = piece(etrangere);

        assertThatThrownBy(() -> service.supprimer(p.getId())).isInstanceOf(BusinessException.class);
        verify(fichiers, never()).delete(any());
    }
}
