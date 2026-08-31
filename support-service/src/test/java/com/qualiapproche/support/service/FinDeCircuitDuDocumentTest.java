package com.qualiapproche.support.service;

import com.qualiapproche.storage.StorageService;
import com.qualiapproche.support.client.WorkflowClient;
import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import com.qualiapproche.support.repository.DocumentStructureAccessRepository;
import com.qualiapproche.support.repository.DocumentUserAccessRepository;
import com.qualiapproche.support.repository.QmsDocumentVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ce qu'un circuit mené à son terme fait du document.
 *
 * <p>Le moteur ne publie pas une seule issue finale : la nature de la décision qui clôt le circuit
 * décide du mot employé — {@code APPROVED} pour une approbation, {@code CLOSED} pour une clôture,
 * {@code REJECTED} pour un rejet. Seules les deux premières disent la même chose du document :
 * plus personne n'a de décision à prendre, et le texte n'a pas été refusé.</p>
 *
 * <p>La clôture n'était traitée nulle part. Comme l'éditeur de circuits propose cette nature pour
 * toutes les familles, un circuit documentaire monté ainsi conduisait son document jusqu'au bout
 * sans jamais le mettre en vigueur : il restait affiché à l'étape « CLOTURE », absent des documents
 * en vigueur, et aucune demande de modification ne pouvait le viser — l'écran de dépôt ne propose
 * que ceux-là.</p>
 */
class FinDeCircuitDuDocumentTest {

    private static final UUID DOCUMENT = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final String STRUCTURE = "structure-1";

    private DocumentQmsRepository documentRepository;
    private ProfilUtilisateurService profilService;
    private QmsDocumentService service;

    @BeforeEach
    void setUp() throws Exception {
        documentRepository = mock(DocumentQmsRepository.class);
        profilService = mock(ProfilUtilisateurService.class);
        NiveauxConfidentialiteService niveauxService = mock(NiveauxConfidentialiteService.class);

        service = new QmsDocumentService(
                documentRepository,
                mock(QmsDocumentVersionRepository.class),
                mock(DocumentUserAccessRepository.class),
                mock(DocumentStructureAccessRepository.class),
                profilService,
                niveauxService,
                mock(QmsAuditLogService.class),
                mock(StorageService.class),
                mock(QmsDocumentTypeService.class),
                mock(MailService.class),
                mock(WorkflowClient.class),
                mock(EtatsDuCircuitService.class));

        lenient().when(profilService.profilCourant())
                .thenReturn(new ProfilUtilisateurService.Profil(STRUCTURE, java.util.Set.of()));
        lenient().when(niveauxService.peutVoir(any(), any())).thenReturn(true);
        lenient().when(documentRepository.save(any(DocumentQms.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Jwt jwt = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .subject("utilisateur-1")
                .issuedAt(java.time.Instant.EPOCH)
                .expiresAt(java.time.Instant.EPOCH.plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, java.util.List.of()));
    }

    @AfterEach
    void nettoyerContexte() {
        SecurityContextHolder.clearContext();
    }

    /** Un document arrivé à la dernière étape de son circuit, décision non encore appliquée. */
    private DocumentQms documentEnApprobation() {
        DocumentQms document = DocumentQms.builder()
                .documentNumber("PRO-001")
                .titre("Procédure d'achat")
                .documentType("PRO")
                .serviceId(STRUCTURE)
                .serviceSigle("DAF")
                .esTraiter(false)
                .numeroVersion(0)
                .currentEtape("Approbation")
                .periodiciteMois(12)
                .build();
        document.setId(DOCUMENT);
        when(documentRepository.findById(DOCUMENT)).thenReturn(Optional.of(document));
        return document;
    }

    @Test
    @DisplayName("Une clôture met le document en vigueur : c'est bien la fin de son circuit")
    void cloture_metLeDocumentEnVigueur() {
        DocumentQms document = documentEnApprobation();

        service.updateWorkflowStatus(DOCUMENT, "CLOSED", "CLOTURE", "Dossier clos");

        assertThat(document.isEsTraiter()).isTrue();
        assertThat(document.isObsolete()).isFalse();
        assertThat(document.getDateVigueur()).isNotNull();
        assertThat(document.getDateProchRevision()).isNotNull();
    }

    @Test
    @DisplayName("Une approbation met le document en vigueur, comme auparavant")
    void approbation_metLeDocumentEnVigueur() {
        DocumentQms document = documentEnApprobation();

        service.updateWorkflowStatus(DOCUMENT, "APPROVED", "APPROUVE", "Approuvé");

        assertThat(document.isEsTraiter()).isTrue();
        assertThat(document.getDateVigueur()).isNotNull();
    }

    @Test
    @DisplayName("Un rejet renvoie le document en rédaction, il n'entre pas en vigueur")
    void rejet_renvoieEnRedaction() {
        DocumentQms document = documentEnApprobation();

        service.updateWorkflowStatus(DOCUMENT, "REJECTED", "REJETE", "À revoir");

        assertThat(document.isEsTraiter()).isFalse();
        assertThat(document.getCurrentEtape()).isNull();
    }

    @Test
    @DisplayName("Un circuit encore en cours ne fait qu'inscrire l'étape atteinte")
    void circuitEnCours_inscritSeulementLEtape() {
        DocumentQms document = documentEnApprobation();

        service.updateWorkflowStatus(DOCUMENT, "EN_COURS", "Vérification", null);

        assertThat(document.isEsTraiter()).isFalse();
        assertThat(document.getCurrentEtape()).isEqualTo("Vérification");
        assertThat(document.getDateVigueur()).isNull();
    }

    /**
     * Place le contexte de sécurité du rappel de circuit : un compte de service, tel que
     * l'authentifie {@code @perm.appelDeService()}.
     *
     * <p>Ce n'est pas un détail de mise en scène : c'est le seul contexte dans lequel le rappel
     * s'exécute réellement. Les cas ci-dessus s'appuyaient sur un utilisateur rattaché à la
     * structure du document — un contexte que le rappel ne connaît jamais — et validaient donc un
     * chemin que la production n'emprunte pas.</p>
     */
    private void contexteDuRappelDeCircuit() {
        // Un compte de service n'est rattaché à aucune structure et ne porte aucun rôle métier :
        // c'est très exactement ce qui le privait de portée sur le document.
        when(profilService.profilCourant())
                .thenReturn(new ProfilUtilisateurService.Profil(null, java.util.Set.of()));

        Jwt jeton = Jwt.withTokenValue("jeton-de-service")
                .header("alg", "none")
                .subject("11111111-1111-4111-8111-111111111111")
                .claim("preferred_username", "service-account-workflow")
                .issuedAt(java.time.Instant.EPOCH)
                .expiresAt(java.time.Instant.EPOCH.plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jeton, null, java.util.List.of()));
    }

    @Test
    @DisplayName("Le compte de service du circuit met le document en vigueur, sans portée sur lui")
    void rappelDeCircuit_approbation_metEnVigueur() {
        DocumentQms document = documentEnApprobation();
        contexteDuRappelDeCircuit();

        service.updateWorkflowStatus(DOCUMENT, "APPROVED", "APPROUVE", "Approuvé");

        assertThat(document.isEsTraiter()).isTrue();
        assertThat(document.getDateVigueur()).isNotNull();
        assertThat(document.getCurrentEtape()).isEqualTo("APPROUVE");
    }

    @Test
    @DisplayName("Une clôture prononcée par le circuit met elle aussi le document en vigueur")
    void rappelDeCircuit_cloture_metEnVigueur() {
        DocumentQms document = documentEnApprobation();
        contexteDuRappelDeCircuit();

        service.updateWorkflowStatus(DOCUMENT, "CLOSED", "CLOTURE", "Dossier clos");

        assertThat(document.isEsTraiter()).isTrue();
        assertThat(document.getDateVigueur()).isNotNull();
    }

    @Test
    @DisplayName("Un rejet prononcé par le circuit rend bien le document à son rédacteur")
    void rappelDeCircuit_rejet_renvoieEnRedaction() {
        DocumentQms document = documentEnApprobation();
        contexteDuRappelDeCircuit();

        service.updateWorkflowStatus(DOCUMENT, "REJECTED", "REJETE", "À revoir");

        assertThat(document.isEsTraiter()).isFalse();
        assertThat(document.getCurrentEtape()).isNull();
    }
}
