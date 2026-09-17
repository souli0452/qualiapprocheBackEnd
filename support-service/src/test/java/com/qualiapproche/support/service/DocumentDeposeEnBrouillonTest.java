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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un document déposé est un brouillon tant que personne ne l'a soumis.
 *
 * <p>L'écran de création offre deux issues — enregistrer, ou soumettre — mais le dépôt inscrivait
 * l'étape courante dès l'ouverture du circuit : aucun document ne pouvait donc être un brouillon,
 * et les deux boutons produisaient le même résultat. Le rédacteur n'avait aucun moyen de déposer
 * un texte pour y revenir.</p>
 *
 * <p>Ce que ces tests fixent : le circuit s'ouvre bien au dépôt — sans lui, le rédacteur n'aurait
 * aucune décision à prendre le jour où il veut soumettre — mais {@code currentEtape} reste nul,
 * qui est déjà la définition du brouillon pour le statut affiché, pour le filtre des listes et
 * pour le retour en arrière après un rejet.</p>
 */
class DocumentDeposeEnBrouillonTest {

    private static final UUID CIRCUIT = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");

    private DocumentQmsRepository documentRepository;
    private WorkflowClient workflowClient;
    private QmsDocumentTypeService typeService;
    private QmsDocumentService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentQmsRepository.class);
        workflowClient = mock(WorkflowClient.class);
        typeService = mock(QmsDocumentTypeService.class);

        service = new QmsDocumentService(
                documentRepository,
                mock(QmsDocumentVersionRepository.class),
                mock(DocumentUserAccessRepository.class),
                mock(DocumentStructureAccessRepository.class),
                mock(ProfilUtilisateurService.class),
                mock(NiveauxConfidentialiteService.class),
                mock(QmsAuditLogService.class),
                mock(StorageService.class),
                typeService,
                mock(MailService.class),
                workflowClient,
                mock(EtatsDuCircuitService.class));

        QmsDocumentType type = QmsDocumentType.builder()
                .code("PRO").libelle("Procédure").folderName("Procedures").build();
        type.setId(UUID.fromString("11111111-0000-4000-8000-000000000011"));
        when(typeService.getTypeByCode("PRO")).thenReturn(type);

        // Le circuit à ouvrir est la réponse du moteur : c'est lui qui détient la règle du
        // circuit réservé à un type, et à défaut celui de la famille.
        WorkflowSummaryDto circuit = new WorkflowSummaryDto();
        circuit.setId(CIRCUIT);
        when(workflowClient.circuitAOuvrir(eq("DOCUMENT"), any())).thenReturn(circuit);

        // Un circuit qui a des étapes : le dépôt refuse un circuit vide.
        when(workflowClient.getWorkflowById(CIRCUIT))
                .thenReturn(Map.of("nom", "Validation documentaire", "steps", List.of(Map.of("id", 1))));

        // Le moteur rend son instance, ouverte à la première étape : c'est exactement ce que le
        // dépôt ne doit PAS recopier sur le document.
        WorkflowInstanceDto instance = new WorkflowInstanceDto();
        instance.setCurrentStateName("Rédaction");
        lenient().when(workflowClient.initiateWorkflow(any(), eq("DOCUMENT"), eq(CIRCUIT), anyString()))
                .thenReturn(instance);

        when(documentRepository.save(any(DocumentQms.class)))
                .thenAnswer(invocation -> {
                    DocumentQms enregistre = invocation.getArgument(0);
                    if (enregistre.getId() == null) {
                        enregistre.setId(UUID.randomUUID());
                    }
                    return enregistre;
                });
    }

    private DocumentQms deposer() {
        return service.createDocument(
                new MockMultipartFile("file", "procedure.pdf", "application/pdf", "%PDF-".getBytes()),
                "Procédure d'achat", "PRO", null, null,
                "structure-1", "Direction des achats", "DA",
                "Awa Traoré", 12, false, false, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Un document déposé reste un brouillon : aucune étape ne lui est inscrite")
    void documentDepose_resteUnBrouillon() {
        DocumentQms document = deposer();

        assertThat(document.getCurrentEtape()).isNull();
        assertThat(service.getDocumentDisplayState(document)).isEqualTo("BROUILLON");
    }

    @Test
    @DisplayName("Le circuit s'ouvre malgré tout : sans lui, rien ne pourrait être soumis ensuite")
    void documentDepose_ouvreSonCircuit() {
        DocumentQms document = deposer();

        assertThat(document.getWorkflowId()).isEqualTo(CIRCUIT);
        verify(workflowClient).initiateWorkflow(any(), eq("DOCUMENT"), eq(CIRCUIT), anyString());
    }

    @Test
    @DisplayName("Le brouillon n'est ni en vigueur ni en circuit")
    void brouillon_dansAucunCompte() {
        DocumentQms document = deposer();

        assertThat(com.qualiapproche.support.dto.DocumentStatDimension.estEnCircuit(document)).isFalse();
        assertThat(com.qualiapproche.support.dto.DocumentStatDimension.estEnVigueur(document)).isFalse();
    }
}
