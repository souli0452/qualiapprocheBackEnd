package com.qualiapproche.support.service;

import com.qualiapproche.common.utils.PermissionsPortee;
import com.qualiapproche.storage.StorageService;
import com.qualiapproche.support.client.WorkflowClient;
import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.model.DocumentStructureAccess;
import com.qualiapproche.support.model.DocumentUserAccess;
import com.qualiapproche.support.numerotation.NumerotationDesDocuments;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import com.qualiapproche.support.repository.DocumentStructureAccessRepository;
import com.qualiapproche.support.repository.DocumentUserAccessRepository;
import com.qualiapproche.support.repository.QmsDocumentVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Qui voit un document pris isolément, et jusqu'où.
 *
 * <p>Le filtre des recherches ne protège que les listes : la fiche, l'historique, l'archivage et
 * la suppression se gardent document par document. Trois portées — aucune, partage, interne —
 * et un classement opposable à tous, sauf au partage nominatif et à l'administration générale.
 * Un document invisible répond comme un document inexistant : le refus ne doit rien révéler.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VisibiliteDUnDocumentTest {

    private static final String GSI = "structure-gsi";
    private static final String DQA = "structure-dqa";
    private static final String SECRET = "niveau-secret";

    @Mock private DocumentQmsRepository documentRepository;
    @Mock private QmsDocumentVersionRepository versionRepository;
    @Mock private DocumentUserAccessRepository accessRepository;
    @Mock private DocumentStructureAccessRepository structureAccessRepository;
    @Mock private ProfilUtilisateurService profilUtilisateurService;
    @Mock private NiveauxConfidentialiteService niveaux;
    @Mock private QmsAuditLogService auditLogService;
    @Mock private StorageService storageService;
    @Mock private QmsDocumentTypeService typeService;
    @Mock private MailService mailService;
    @Mock private WorkflowClient workflowClient;
    @Mock private EtatsDuCircuitService etatsDuCircuit;
    @Mock private NumerotationDesDocuments numerotation;

    @InjectMocks private QmsDocumentService service;

    private DocumentQms document;

    private static void connecter(String sujet) {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none").subject(sujet).claim("name", "Awa Ouédraogo").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private void profil(String structure, Set<String> roles, String... permissions) {
        when(profilUtilisateurService.profilCourant())
                .thenReturn(new ProfilUtilisateurService.Profil(structure, roles, Set.of(permissions)));
    }

    private DocumentQms document(String structure, String auteur) {
        DocumentQms d = DocumentQms.builder().documentNumber("PR-GSI-001").serviceId(structure).build();
        d.setId(UUID.randomUUID());
        d.setCreatedById(auteur);
        d.setCreatedAt(LocalDateTime.now());
        when(documentRepository.findById(d.getId())).thenReturn(Optional.of(d));
        return d;
    }

    private void partageNominatif(String utilisateur) {
        when(accessRepository.findByDocumentIdAndUserId(document.getId(), utilisateur))
                .thenReturn(Optional.of(DocumentUserAccess.builder().userId(utilisateur).build()));
    }

    private void partageStructure(String structure) {
        when(structureAccessRepository.findByDocumentIdAndStructureId(document.getId(), structure))
                .thenReturn(Optional.of(DocumentStructureAccess.builder().structureId(structure).build()));
    }

    @BeforeEach
    void preparer() {
        connecter("agent-1");
        document = document(GSI, "redacteur-gsi");
        when(niveaux.peutVoir(any(), any())).thenAnswer(i -> i.getArgument(0) == null);
        when(documentRepository.save(any(DocumentQms.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private static void introuvable(org.assertj.core.api.ThrowableAssert.ThrowingCallable geste) {
        assertThatThrownBy(geste).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("introuvable");
    }

    @Nested
    @DisplayName("Portée d'un document")
    class Portee {

        @Test
        @DisplayName("La structure émettrice voit son document et son suivi interne")
        void la_structure_emettrice_voit_tout() {
            profil(GSI, Set.of());

            assertThat(service.getDocumentById(document.getId())).isSameAs(document);
            assertThat(service.peutVoirLeSuiviInterne(document)).isTrue();
        }

        @Test
        @DisplayName("Une autre structure, sans partage, ne voit pas le document : il est « introuvable »")
        void une_autre_structure_ne_voit_pas() {
            profil(DQA, Set.of());

            introuvable(() -> service.getDocumentById(document.getId()));
            introuvable(() -> service.getVersionHistory(document.getId()));
            assertThat(service.peutVoirLeSuiviInterne(document)).isFalse();
        }

        @Test
        @DisplayName("Un partage à la structure ouvre la consultation, pas l'historique ni la piste d'audit")
        void un_partage_ouvre_la_consultation_seulement() {
            profil(DQA, Set.of());
            partageStructure(DQA);

            assertThat(service.getDocumentById(document.getId())).isSameAs(document);
            assertThatThrownBy(() -> service.getVersionHistory(document.getId()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> service.getAuditLogs(document.getId()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThat(service.peutVoirLeSuiviInterne(document)).isFalse();
        }

        @Test
        @DisplayName("Le destinataire d'un partage n'archive ni ne supprime le document")
        void le_destinataire_d_un_partage_n_archive_pas() {
            profil(DQA, Set.of());
            partageNominatif("agent-1");

            assertThatThrownBy(() -> service.archiveDocument(document.getId(), "fin de vie"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> service.deleteDocument(document.getId(), "erreur"))
                    .isInstanceOf(AccessDeniedException.class);
            verify(documentRepository, never()).delete(any(DocumentQms.class));
            assertThat(document.isArchived()).isFalse();
        }

        @Test
        @DisplayName("L'auteur voit son document même hors de sa structure")
        void l_auteur_voit_son_document() {
            connecter("redacteur-gsi");
            profil(DQA, Set.of());

            assertThat(service.peutVoirLeSuiviInterne(document)).isTrue();
        }

        @Test
        @DisplayName("Sans utilisateur authentifié, aucun document n'existe")
        void sans_utilisateur_rien_n_existe() {
            SecurityContextHolder.clearContext();
            profil(GSI, Set.of(), PermissionsPortee.TOUTES_STRUCTURES);

            introuvable(() -> service.getDocumentById(document.getId()));
        }

        @Test
        @DisplayName("La portée toutes structures voit le document d'une autre structure")
        void la_portee_transverse_voit_tout() {
            profil(DQA, Set.of(), PermissionsPortee.TOUTES_STRUCTURES);

            assertThat(service.peutVoirLeSuiviInterne(document)).isTrue();
        }
    }

    @Nested
    @DisplayName("Classement opposable")
    class Classement {

        @BeforeEach
        void classer() {
            document.setNiveauConfidentialiteId(SECRET);
            when(niveaux.peutVoir(SECRET, Set.of("DIRECTEUR"))).thenReturn(true);
        }

        @Test
        @DisplayName("Le classement s'oppose à la structure émettrice elle-même, sans le rôle admis")
        void le_classement_s_oppose_a_la_structure() {
            profil(GSI, Set.of("AGENT"));

            introuvable(() -> service.getDocumentById(document.getId()));
        }

        @Test
        @DisplayName("Voir toutes les structures ne dispense pas du classement")
        void la_portee_transverse_ne_dispense_pas_du_classement() {
            profil(GSI, Set.of("AGENT"), PermissionsPortee.TOUTES_STRUCTURES);

            introuvable(() -> service.getDocumentById(document.getId()));
        }

        @Test
        @DisplayName("Le rôle admis par le niveau ouvre le document")
        void le_role_admis_ouvre() {
            profil(GSI, Set.of("DIRECTEUR"));

            assertThat(service.getDocumentById(document.getId())).isSameAs(document);
        }

        @Test
        @DisplayName("Le partage nominatif lève le classement, le partage à la structure non")
        void le_partage_nominatif_leve_le_classement() {
            profil(DQA, Set.of("AGENT"));
            partageStructure(DQA);
            introuvable(() -> service.getDocumentById(document.getId()));

            partageNominatif("agent-1");
            assertThat(service.getDocumentById(document.getId())).isSameAs(document);
        }

        @Test
        @DisplayName("L'administration générale échappe au classement, pour qu'un document mal classé reste réparable")
        void l_administration_echappe_au_classement() {
            profil(DQA, Set.of("AGENT"), PermissionsPortee.HORS_CLASSEMENT, PermissionsPortee.TOUTES_STRUCTURES);

            assertThat(service.peutVoirLeSuiviInterne(document)).isTrue();
        }

        @Test
        @DisplayName("Un document classé n'apparaît pas à traiter pour qui ne peut pas le voir")
        void un_document_classe_n_apparait_pas_a_traiter() {
            profil(GSI, Set.of("AGENT"));
            DocumentQms visible = document(GSI, "redacteur-gsi");
            visible.setCreatedAt(LocalDateTime.now().minusDays(2));
            DocumentQms recent = document(GSI, "redacteur-gsi");
            recent.setCreatedAt(LocalDateTime.now());
            when(etatsDuCircuit.ressourcesADecider("DOCUMENT"))
                    .thenReturn(List.of(document.getId(), visible.getId(), recent.getId()));
            when(documentRepository.findAllById(any())).thenReturn(List.of(document, visible, recent));

            assertThat(service.aTraiterParLAppelant()).containsExactly(recent, visible);
        }
    }

    @Nested
    @DisplayName("Reclassement")
    class Reclassement {

        @Test
        @DisplayName("Sans la permission de classer, le niveau ne change pas")
        void sans_permission_pas_de_reclassement() {
            profil(GSI, Set.of("AGENT"), PermissionsPortee.TOUTES_STRUCTURES);

            assertThatThrownBy(() -> service.reclasser(document.getId(), SECRET, "Secret"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThat(document.getNiveauConfidentialiteId()).isNull();
            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Classer un document inscrit son rédacteur parmi les accès nominatifs, et le trace")
        void classer_inscrit_le_redacteur() {
            profil(GSI, Set.of(), "niveau-confidentialite-write");
            document.setRedacteur("Issa Kaboré");

            service.reclasser(document.getId(), SECRET, "Secret");

            assertThat(document.isConfidentiel()).isTrue();
            assertThat(document.getNiveauConfidentialiteLibelle()).isEqualTo("Secret");
            ArgumentCaptor<DocumentUserAccess> acces = ArgumentCaptor.forClass(DocumentUserAccess.class);
            verify(accessRepository).save(acces.capture());
            assertThat(acces.getValue().getUserId()).isEqualTo("redacteur-gsi");
            assertThat(acces.getValue().getUserFullName()).isEqualTo("Issa Kaboré");
            verify(auditLogService).logAction(eq("RECLASSEMENT"), eq("PR-GSI-001"), contains("« aucun » → « Secret »"));
        }

        @Test
        @DisplayName("Le rédacteur déjà inscrit ne l'est pas deux fois")
        void le_redacteur_n_est_pas_inscrit_deux_fois() {
            profil(GSI, Set.of(), "niveau-confidentialite-write");
            when(accessRepository.findByDocumentIdAndUserId(document.getId(), "redacteur-gsi"))
                    .thenReturn(Optional.of(DocumentUserAccess.builder().userId("redacteur-gsi").build()));

            service.reclasser(document.getId(), SECRET, "Secret");

            verify(accessRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un niveau vide déclasse le document")
        void un_niveau_vide_declasse() {
            profil(GSI, Set.of(), PermissionsPortee.HORS_CLASSEMENT);
            document.setNiveauConfidentialiteId(SECRET);
            document.setNiveauConfidentialiteLibelle("Secret");
            document.setConfidentiel(true);

            assertThat(service.reclasser(document.getId(), " ", null)).isNull();

            assertThat(document.getNiveauConfidentialiteId()).isNull();
            assertThat(document.isConfidentiel()).isFalse();
            verify(accessRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un classement qui exclut les décideurs du circuit est averti, sans être refusé")
        void un_classement_qui_ferme_le_circuit_est_averti() {
            profil(GSI, Set.of(), "niveau-confidentialite-write");
            document.setWorkflowId(UUID.randomUUID());
            when(niveaux.rolesAdmis(SECRET)).thenReturn(Set.of("DIRECTEUR"));
            when(workflowClient.getWorkflowById(document.getWorkflowId())).thenReturn(Map.of("steps", List.of(
                    Map.of("responsableRole", "directeur"),
                    Map.of("responsableRole", "PILOTE",
                            "transitions", List.of(Map.of("requiredRole", "RQ"))))));

            String avertissement = service.reclasser(document.getId(), SECRET, "Secret");

            assertThat(avertissement).contains("PILOTE, RQ").contains("qui décident").doesNotContain("DIRECTEUR,");
            assertThat(document.getNiveauConfidentialiteId()).isEqualTo(SECRET);
        }

        @Test
        @DisplayName("Un circuit illisible ne produit pas d'avertissement inventé")
        void un_circuit_illisible_ne_produit_pas_d_avertissement() {
            profil(GSI, Set.of(), "niveau-confidentialite-write");
            document.setWorkflowId(UUID.randomUUID());
            when(niveaux.rolesAdmis(SECRET)).thenReturn(Set.of("DIRECTEUR"));
            when(workflowClient.getWorkflowById(any())).thenThrow(new RuntimeException("workflow-service muet"));

            assertThat(service.reclasser(document.getId(), SECRET, "Secret")).isNull();
        }
    }

    @Nested
    @DisplayName("Archivage et suppression")
    class ArchivageEtSuppression {

        @BeforeEach
        void structureEmettrice() {
            profil(GSI, Set.of());
        }

        @Test
        @DisplayName("Un document entré en vigueur ne se supprime pas : il s'archive")
        void un_document_en_vigueur_ne_se_supprime_pas() {
            document.setDateVigueur(LocalDateTime.now());

            assertThatThrownBy(() -> service.deleteDocument(document.getId(), "doublon"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
            verify(documentRepository, never()).delete(any(DocumentQms.class));
        }

        @Test
        @DisplayName("Un document validé par le circuit ne se supprime pas non plus")
        void un_document_traite_ne_se_supprime_pas() {
            document.setEsTraiter(true);

            assertThatThrownBy(() -> service.deleteDocument(document.getId(), "doublon"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Un brouillon se supprime, et la piste d'audit garde son numéro et le motif")
        void un_brouillon_se_supprime_en_laissant_sa_trace() {
            service.deleteDocument(document.getId(), "création erronée");

            verify(documentRepository).delete(document);
            verify(auditLogService).logAction(eq("SUPPRESSION"), eq("PR-GSI-001"), contains("création erronée"));
        }

        @Test
        @DisplayName("Archiver consigne qui et pourquoi ; réarchiver ne retrace rien")
        void archiver_consigne_l_auteur_et_le_motif() {
            service.archiveDocument(document.getId(), "Remplacé par la v3");

            assertThat(document.isArchived()).isTrue();
            assertThat(document.getLastModifiedBy()).isEqualTo("Awa Ouédraogo");
            assertThat(document.getLastModifiedReason()).isEqualTo("Remplacé par la v3");

            service.archiveDocument(document.getId(), "encore");
            verify(auditLogService).logAction(eq("ARCHIVAGE"), anyString(), anyString());
            assertThat(document.getLastModifiedReason()).isEqualTo("Remplacé par la v3");
        }
    }
}
