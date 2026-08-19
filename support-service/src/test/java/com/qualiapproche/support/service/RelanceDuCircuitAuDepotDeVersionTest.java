package com.qualiapproche.support.service;

import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.storage.StorageService;
import com.qualiapproche.support.client.WorkflowClient;
import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.model.QmsDocumentType;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import com.qualiapproche.support.repository.DocumentStructureAccessRepository;
import com.qualiapproche.support.repository.DocumentUserAccessRepository;
import com.qualiapproche.support.repository.QmsDocumentVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un document validé dont le contenu change doit repasser par son circuit de validation
 * (ISO 9001 §7.5.2) — c'est tout le sens d'une demande de modification aboutie.
 *
 * <p>Ces tests fixent la chaîne de replis qui garantit la relance : l'instance passée que le moteur
 * connaît, à défaut le circuit resté attaché au document, à défaut le circuit configuré pour son
 * type. Le retour en brouillon sans circuit n'est plus que l'ultime extrémité, quand rien de tout
 * cela n'existe — auparavant, le premier accroc suffisait à l'y faire retomber en silence, et le
 * document modifié ne resuivait jamais sa validation.</p>
 */
class RelanceDuCircuitAuDepotDeVersionTest {

    private static final UUID DOCUMENT = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID CIRCUIT_SUIVI = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    private static final UUID CIRCUIT_ATTACHE = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000002");
    private static final UUID CIRCUIT_CONFIGURE = UUID.fromString("cccccccc-0000-4000-8000-000000000003");
    private static final UUID TYPE_PRO = UUID.fromString("dddddddd-0000-4000-8000-000000000004");
    private static final String STRUCTURE = "structure-1";

    private DocumentQmsRepository documentRepository;
    private QmsDocumentVersionRepository versionRepository;
    private ProfilUtilisateurService profilService;
    private NiveauxConfidentialiteService niveauxService;
    private QmsDocumentTypeService typeService;
    private WorkflowClient workflowClient;
    private QmsDocumentService service;

    @BeforeEach
    void setUp() throws Exception {
        documentRepository = mock(DocumentQmsRepository.class);
        versionRepository = mock(QmsDocumentVersionRepository.class);
        profilService = mock(ProfilUtilisateurService.class);
        niveauxService = mock(NiveauxConfidentialiteService.class);
        typeService = mock(QmsDocumentTypeService.class);
        workflowClient = mock(WorkflowClient.class);
        StorageService storageService = mock(StorageService.class);

        service = new QmsDocumentService(
                documentRepository,
                versionRepository,
                mock(DocumentUserAccessRepository.class),
                mock(DocumentStructureAccessRepository.class),
                profilService,
                niveauxService,
                mock(QmsAuditLogService.class),
                storageService,
                typeService,
                mock(MailService.class),
                workflowClient,
                mock(EtatsDuCircuitService.class));

        lenient().when(profilService.profilCourant())
                .thenReturn(new ProfilUtilisateurService.Profil(STRUCTURE, java.util.Set.of()));
        lenient().when(niveauxService.peutVoir(any(), any())).thenReturn(true);
        lenient().when(typeService.getTypeByCode("PRO")).thenReturn(typeProcedure());
        lenient().when(storageService.uploadFile(any(), anyString(), anyString()))
                .thenReturn("procedures/PRO-001-v1.pdf");
        lenient().when(documentRepository.save(any(DocumentQms.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

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

    private QmsDocumentType typeProcedure() {
        QmsDocumentType type = QmsDocumentType.builder()
                .code("PRO")
                .libelle("Procédure")
                .folderName("procedures")
                .build();
        type.setId(TYPE_PRO);
        return type;
    }

    private DocumentQms documentValide(UUID circuitAttache) {
        DocumentQms document = DocumentQms.builder()
                .documentNumber("PRO-001")
                .titre("Procédure d'achat")
                .documentType("PRO")
                .serviceId(STRUCTURE)
                .serviceSigle("DAF")
                .esTraiter(true)
                .numeroVersion(0)
                .workflowId(circuitAttache)
                .build();
        document.setId(DOCUMENT);
        when(documentRepository.findById(DOCUMENT)).thenReturn(Optional.of(document));
        return document;
    }

    private MockMultipartFile fichier() {
        return new MockMultipartFile("file", "PRO-001-v1.pdf", "application/pdf", "contenu".getBytes());
    }

    private WorkflowInstanceDto instanceOuverte() {
        WorkflowInstanceDto instance = new WorkflowInstanceDto();
        instance.setCurrentStateName("Rédaction");
        return instance;
    }

    @Test
    @DisplayName("Le circuit suivi la dernière fois est relancé quand le moteur le connaît")
    void instancePasseeConnue_relanceLeMemeCircuit() throws Exception {
        DocumentQms document = documentValide(null);
        WorkflowInstanceDto passee = new WorkflowInstanceDto();
        passee.setWorkflowId(CIRCUIT_SUIVI);
        when(workflowClient.getLastValidationInstance(DOCUMENT)).thenReturn(passee);
        when(workflowClient.initiateWorkflow(DOCUMENT, "DOCUMENT", CIRCUIT_SUIVI, "PRO-001"))
                .thenReturn(instanceOuverte());

        service.addVersion(DOCUMENT, fichier(), "révision", true);

        verify(workflowClient).initiateWorkflow(DOCUMENT, "DOCUMENT", CIRCUIT_SUIVI, "PRO-001");
        assertThat(document.getCurrentEtape()).isEqualTo("Rédaction");
        assertThat(document.getNumeroVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Moteur muet sur le passé : le circuit resté attaché au document est relancé")
    void instancePasseeIllisible_leCircuitAttacheEstRelance() throws Exception {
        DocumentQms document = documentValide(CIRCUIT_ATTACHE);
        when(workflowClient.getLastValidationInstance(DOCUMENT)).thenThrow(new RuntimeException("indisponible"));
        when(workflowClient.initiateWorkflow(DOCUMENT, "DOCUMENT", CIRCUIT_ATTACHE, "PRO-001"))
                .thenReturn(instanceOuverte());

        service.addVersion(DOCUMENT, fichier(), "révision", true);

        verify(workflowClient).initiateWorkflow(DOCUMENT, "DOCUMENT", CIRCUIT_ATTACHE, "PRO-001");
        assertThat(document.isEsTraiter())
                .as("la version en vigueur le reste pendant que la révision se valide")
                .isTrue();
        assertThat(document.getCurrentEtape()).isEqualTo("Rédaction");
    }

    @Test
    @DisplayName("Ni instance passée ni circuit attaché : le circuit configuré pour le type est relancé")
    void sansPasseNiAttache_leCircuitConfigureEstRelance() throws Exception {
        DocumentQms document = documentValide(null);
        when(workflowClient.getLastValidationInstance(DOCUMENT)).thenThrow(new RuntimeException("indisponible"));
        when(workflowClient.circuitAOuvrir(eq("DOCUMENT"), eq(TYPE_PRO.toString())))
                .thenReturn(new WorkflowSummaryDto(CIRCUIT_CONFIGURE, "Circuit documentaire", "DOCUMENT", true, null));
        when(workflowClient.initiateWorkflow(DOCUMENT, "DOCUMENT", CIRCUIT_CONFIGURE, "PRO-001"))
                .thenReturn(instanceOuverte());

        service.addVersion(DOCUMENT, fichier(), "révision", true);

        verify(workflowClient).initiateWorkflow(DOCUMENT, "DOCUMENT", CIRCUIT_CONFIGURE, "PRO-001");
        assertThat(document.getWorkflowId()).isEqualTo(CIRCUIT_CONFIGURE);
    }

    @Test
    @DisplayName("Aucun circuit nulle part : retour en brouillon, dernier recours seulement")
    void aucunCircuitDisponible_retourEnBrouillon() throws Exception {
        DocumentQms document = documentValide(null);
        when(workflowClient.getLastValidationInstance(DOCUMENT)).thenThrow(new RuntimeException("indisponible"));
        when(workflowClient.circuitAOuvrir(anyString(), any())).thenThrow(new RuntimeException("aucun circuit"));

        service.addVersion(DOCUMENT, fichier(), "révision", true);

        verify(workflowClient, never()).initiateWorkflow(any(), anyString(), any(), anyString());
        assertThat(document.isEsTraiter()).isFalse();
        assertThat(document.getCurrentEtape()).isNull();
    }
}
