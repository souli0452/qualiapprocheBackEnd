package com.qualiapproche.support.service;

import com.qualiapproche.storage.StorageService;
import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.support.client.WorkflowClient;
import com.qualiapproche.support.model.DemandeDocument;
import com.qualiapproche.support.model.DemandeDocument.EtatDemande;
import com.qualiapproche.support.model.DemandeDocument.TypeDemande;
import com.qualiapproche.support.repository.DemandeDocumentRepository;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * L'aboutissement d'une demande est ce qui la distingue d'un simple changement d'état, et c'est là
 * que se jouent les conséquences irréversibles : un document retiré, une version majeure publiée.
 *
 * <p>Ces tests fixent qui déclenche quoi. Une suppression acceptée s'exécute d'elle-même ; une
 * modification acceptée attend son fichier, et rien ne doit être déposé avant que la demande n'ait
 * été instruite.</p>
 */
class DemandeDocumentServiceTest {

    private static final UUID DOCUMENT = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID DEMANDE = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private DemandeDocumentRepository demandeRepository;
    private DocumentQmsRepository documentRepository;
    private QmsDocumentService documentService;
    private ProfilUtilisateurService profilService;
    private StorageService storageService;
    private QmsAuditLogService auditLogService;
    private WorkflowClient workflowClient;
    private DemandeDocumentService service;

    @BeforeEach
    void setUp() {
        demandeRepository = mock(DemandeDocumentRepository.class);
        documentRepository = mock(DocumentQmsRepository.class);
        documentService = mock(QmsDocumentService.class);
        profilService = mock(ProfilUtilisateurService.class);
        storageService = mock(StorageService.class);
        auditLogService = mock(QmsAuditLogService.class);
        workflowClient = mock(WorkflowClient.class);

        service = new DemandeDocumentService(demandeRepository, documentRepository, documentService,
                profilService, storageService, auditLogService, workflowClient);

        when(demandeRepository.save(any(DemandeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Par défaut, l'appelant relève de la structure de la demande : les tests qui portent sur
        // l'aboutissement n'ont pas à se préoccuper de la portée.
        lenient().when(profilService.profilCourant())
                .thenReturn(new ProfilUtilisateurService.Profil(STRUCTURE, java.util.Set.of()));
        authentifier(DEMANDEUR);
    }

    private static final String STRUCTURE = "structure-1";
    private static final String AUTRE_STRUCTURE = "structure-2";
    private static final String DEMANDEUR = "utilisateur-1";
    private static final String TIERS = "utilisateur-2";

    private void authentifier(String userId) {
        org.springframework.security.oauth2.jwt.Jwt jwt =
                org.springframework.security.oauth2.jwt.Jwt.withTokenValue("jeton")
                        .header("alg", "none")
                        .subject(userId)
                        .issuedAt(java.time.Instant.EPOCH)
                        .expiresAt(java.time.Instant.EPOCH.plusSeconds(3600))
                        .build();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        jwt, null, java.util.List.of()));
    }

    @org.junit.jupiter.api.AfterEach
    void nettoyerContexte() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private DemandeDocument demande(TypeDemande type, EtatDemande etat) {
        DemandeDocument demande = DemandeDocument.builder()
                .documentId(DOCUMENT)
                .documentNumber("PRO-DSI-2026-001")
                .type(type)
                .etat(etat)
                .objectif("Mettre à jour la procédure")
                .structureId(STRUCTURE)
                .demandeurId(DEMANDEUR)
                .build();
        demande.setId(DEMANDE);
        return demande;
    }

    private void profil(String structureId, String... roles) {
        when(profilService.profilCourant())
                .thenReturn(new ProfilUtilisateurService.Profil(structureId, java.util.Set.of(roles)));
    }

    @Test
    @DisplayName("Une demande d'une autre structure est déclarée introuvable")
    void demandeDUneAutreStructure_introuvable() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.MODIFICATION, EtatDemande.EN_COURS)));
        profil(AUTRE_STRUCTURE);
        // Un tiers : ni de la structure, ni l'auteur de la demande.
        authentifier(TIERS);

        // « Introuvable » et non « refusé » : répondre « accès refusé » confirmerait à qui n'y a
        // pas droit qu'une demande porte bien cet identifiant.
        assertThatThrownBy(() -> service.getById(DEMANDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    @DisplayName("Une demande de ma structure est consultable")
    void demandeDeMaStructure_consultable() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.MODIFICATION, EtatDemande.EN_COURS)));
        profil(STRUCTURE);

        assertThat(service.getById(DEMANDE).getObjectif()).isEqualTo("Mettre à jour la procédure");
    }

    @Test
    @DisplayName("Le responsable qualité voit les demandes de toutes les structures")
    void responsableQualite_voitToutesLesStructures() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.SUPPRESSION, EtatDemande.EN_COURS)));
        profil(AUTRE_STRUCTURE, "RESPONSABLE_QUALITE");

        assertThat(service.getById(DEMANDE)).isNotNull();
    }

    @Test
    @DisplayName("Le super administrateur voit les demandes de toutes les structures")
    void superAdmin_voitToutesLesStructures() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.MODIFICATION, EtatDemande.EN_COURS)));
        // Ni de la structure de la demande, ni son auteur : seul son rôle le porte.
        profil(AUTRE_STRUCTURE, "SUPER_ADMIN");
        authentifier(TIERS);

        assertThat(service.getById(DEMANDE)).isNotNull();
    }

    @Test
    @DisplayName("Le super administrateur liste les demandes de toutes les structures")
    void superAdmin_listeToutesLesDemandes() {
        profil(AUTRE_STRUCTURE, "SUPER_ADMIN");
        authentifier(TIERS);
        when(demandeRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(java.util.List.of(demande(TypeDemande.MODIFICATION, EtatDemande.EN_COURS)));

        assertThat(service.mesDemandes()).hasSize(1);
        // La liste n'est pas restreinte à sa structure : c'est bien le balayage complet qui est
        // interrogé, non la recherche par structure.
        verify(demandeRepository, never()).findByStructureIdOrderByCreatedAtDesc(anyString());
    }

    @Test
    @DisplayName("Le demandeur garde accès à sa demande même s'il change de structure")
    void demandeur_gardeAccesALaSienne() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.MODIFICATION, EtatDemande.EN_COURS)));
        profil(AUTRE_STRUCTURE);
        authentifier(DEMANDEUR);

        assertThat(service.getById(DEMANDE)).isNotNull();
    }

    @Test
    @DisplayName("Déposer un remplaçant sur la demande d'une autre structure est impossible")
    void remplacant_refuseHorsStructure() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.MODIFICATION, EtatDemande.ACCEPTEE)));
        profil(AUTRE_STRUCTURE);
        authentifier(TIERS);

        // C'est l'acte le plus lourd de cet écran : il publie une version sur le document.
        assertThatThrownBy(() -> service.deposerRemplacant(DEMANDE,
                mock(org.springframework.web.multipart.MultipartFile.class), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    @DisplayName("Suppression acceptée : le document est retiré, la demande passe à exécutée")
    void suppressionAcceptee_retireLeDocument() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.SUPPRESSION, EtatDemande.EN_COURS)));
        when(documentService.supprimerSurDecision(eq(DOCUMENT), any())).thenReturn("PRO-DSI-2026-001");

        service.traiterAvancement(DEMANDE, Map.of("status", "APPROVED", "comments", "Document obsolète"));

        verify(documentService).supprimerSurDecision(eq(DOCUMENT), eq("Document obsolète"));
    }

    @Test
    @DisplayName("Modification acceptée : rien n'est retiré, le fichier remplaçant reste attendu")
    void modificationAcceptee_nExecuteRien() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.MODIFICATION, EtatDemande.EN_COURS)));

        service.traiterAvancement(DEMANDE, Map.of("status", "APPROVED", "comments", "Accordé"));

        // Seul un humain peut fournir le document remplaçant : l'acceptation ne l'invente pas.
        verify(documentService, never()).supprimerSurDecision(any(), any());
    }

    @Test
    @DisplayName("Demande refusée : le document n'est pas touché")
    void demandeRefusee_neToucheRien() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.SUPPRESSION, EtatDemande.EN_COURS)));

        service.traiterAvancement(DEMANDE, Map.of("status", "REJECTED", "comments", "Non justifié"));

        verify(documentService, never()).supprimerSurDecision(any(), any());
    }

    @Test
    @DisplayName("Le remplaçant est refusé tant que la demande n'a pas été acceptée")
    void remplacant_refuseAvantAcceptation() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.MODIFICATION, EtatDemande.EN_COURS)));

        assertThatThrownBy(() -> service.deposerRemplacant(DEMANDE, mock(org.springframework.web.multipart.MultipartFile.class), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("acceptée");
    }

    @Test
    @DisplayName("Le remplaçant est refusé sur une demande de suppression")
    void remplacant_refuseSurUneSuppression() {
        when(demandeRepository.findById(DEMANDE))
                .thenReturn(Optional.of(demande(TypeDemande.SUPPRESSION, EtatDemande.ACCEPTEE)));

        assertThatThrownBy(() -> service.deposerRemplacant(DEMANDE, mock(org.springframework.web.multipart.MultipartFile.class), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pas une demande de modification");
    }

    @Test
    @DisplayName("Sans circuit configuré pour les demandes, le dépôt est refusé et expliqué")
    void sansCircuit_depotRefuse() {
        com.qualiapproche.support.model.DocumentQms document = new com.qualiapproche.support.model.DocumentQms();
        document.setId(DOCUMENT);
        document.setDocumentNumber("PRO-DSI-2026-001");
        when(documentRepository.findById(DOCUMENT)).thenReturn(Optional.of(document));
        when(documentService.peutVoirLeSuiviInterne(document)).thenReturn(true);
        when(profilService.profilCourant())
                .thenReturn(new ProfilUtilisateurService.Profil("structure-1", java.util.Set.of()));
        when(workflowClient.getActiveWorkflowByType(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.creer(DOCUMENT, TypeDemande.MODIFICATION, "Objectif", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucun circuit actif");
    }

    @Test
    @DisplayName("L'objectif est obligatoire : c'est lui qu'instruit le responsable qualité")
    void objectifObligatoire() {
        com.qualiapproche.support.model.DocumentQms document = new com.qualiapproche.support.model.DocumentQms();
        document.setId(DOCUMENT);
        when(documentRepository.findById(DOCUMENT)).thenReturn(Optional.of(document));
        when(documentService.peutVoirLeSuiviInterne(document)).thenReturn(true);

        assertThatThrownBy(() -> service.creer(DOCUMENT, TypeDemande.MODIFICATION, "   ", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("obligatoire");
    }

    @Test
    @DisplayName("Un avancement portant sur une demande inconnue est ignoré sans lever")
    void avancementInconnu_ignore() {
        when(demandeRepository.findById(DEMANDE)).thenReturn(Optional.empty());

        service.traiterAvancement(DEMANDE, Map.of("status", "APPROVED"));

        verify(documentService, never()).supprimerSurDecision(any(), any());
    }

    @Test
    @DisplayName("Un circuit ouvert renseigne l'étape courante de la demande")
    void creation_ouvreLeCircuit() {
        com.qualiapproche.support.model.DocumentQms document = new com.qualiapproche.support.model.DocumentQms();
        document.setId(DOCUMENT);
        document.setDocumentNumber("PRO-DSI-2026-001");
        document.setServiceId("structure-1");
        when(documentRepository.findById(DOCUMENT)).thenReturn(Optional.of(document));
        when(documentService.peutVoirLeSuiviInterne(document)).thenReturn(true);
        when(profilService.profilCourant())
                .thenReturn(new ProfilUtilisateurService.Profil("structure-1", java.util.Set.of()));

        UUID circuit = UUID.randomUUID();
        WorkflowSummaryDto actif = new WorkflowSummaryDto();
        actif.setId(circuit);
        when(workflowClient.getActiveWorkflowByType("DEMANDE_DOCUMENT")).thenReturn(actif);

        WorkflowInstanceDto instance = new WorkflowInstanceDto();
        instance.setCurrentStateName("Soumission de la demande");
        when(workflowClient.initiateWorkflow(any(), eq("DEMANDE_DOCUMENT"), eq(circuit))).thenReturn(instance);

        DemandeDocument creee = service.creer(DOCUMENT, TypeDemande.MODIFICATION, "Objectif", "Détail", null);

        assertThat(creee.getWorkflowId()).isEqualTo(circuit);
        assertThat(creee.getCurrentEtape()).isEqualTo("Soumission de la demande");
        assertThat(creee.getEtat()).isEqualTo(EtatDemande.EN_COURS);
    }
}
