package com.qualiapproche.support.service;

import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.storage.StorageService;
import com.qualiapproche.support.client.WorkflowClient;
import com.qualiapproche.support.model.QmsDocumentType;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import com.qualiapproche.support.repository.DocumentStructureAccessRepository;
import com.qualiapproche.support.repository.DocumentUserAccessRepository;
import com.qualiapproche.support.repository.QmsDocumentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un circuit par type de document, et un repli pour les types qui n'en désignent aucun.
 *
 * <p>C'est la configuration que l'on attend d'un système documentaire : une procédure ne se valide
 * pas comme un enregistrement. Le lien vit sur le <b>circuit</b>, qui se réserve à un type
 * ({@code Workflow.cibleId}) ; le type, lui, ne désigne plus rien. Un seul dépositaire, donc aucune
 * contradiction possible.</p>
 *
 * <p>Ces tests fixent ce que support-service demande au moteur — la famille et l'identifiant du type
 * — et ce qu'il fait de sa réponse. La règle elle-même est éprouvée chez le moteur, qui la
 * détient.</p>
 */
class UnCircuitParTypeDeDocumentTest {

    /** Circuit livré au premier démarrage : le repli de tous les types qui ne désignent rien. */
    private static final UUID CIRCUIT_LIVRE = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    /** Circuits ajoutés ensuite, chacun attribué à un type. */
    private static final UUID CIRCUIT_PROCEDURES = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000002");
    /** Identifiants de types de documents : c'est par eux qu'un circuit se réserve. */
    private static final UUID TYPE_PRO = UUID.fromString("11111111-0000-4000-8000-000000000011");
    private static final UUID TYPE_ENR = UUID.fromString("22222222-0000-4000-8000-000000000022");

    private WorkflowClient workflowClient;
    private QmsDocumentService service;

    @BeforeEach
    void setUp() {
        workflowClient = mock(WorkflowClient.class);

        // Seuls le client de circuits et le type de document interviennent dans la résolution ; le
        // reste du service — dépôt du fichier, audit, accès — n'y a aucune part.
        service = new QmsDocumentService(
                mock(DocumentQmsRepository.class),
                mock(QmsDocumentVersionRepository.class),
                mock(DocumentUserAccessRepository.class),
                mock(DocumentStructureAccessRepository.class),
                mock(ProfilUtilisateurService.class),
                mock(NiveauxConfidentialiteService.class),
                mock(QmsAuditLogService.class),
                mock(StorageService.class),
                mock(QmsDocumentTypeService.class),
                mock(MailService.class),
                workflowClient,
                mock(EtatsDuCircuitService.class));
    }

    private QmsDocumentType type(String code, String libelle, UUID id) {
        QmsDocumentType type = QmsDocumentType.builder()
                .code(code)
                .libelle(libelle)
                .folderName(libelle)
                .build();
        type.setId(id);
        return type;
    }

    /**
     * Ce que rend le moteur pour un type donné : le circuit qui lui est réservé, ou celui par défaut.
     *
     * <p>La règle est appliquée là-bas, pas ici — c'est tout l'intérêt du point d'entrée unique. Le
     * test se borne donc à dire ce que le moteur répond.</p>
     */
    private void circuitRenduParLeMoteur(UUID id) {
        WorkflowSummaryDto circuit = new WorkflowSummaryDto();
        circuit.setId(id);
        when(workflowClient.circuitAOuvrir(eq("DOCUMENT"), any())).thenReturn(circuit);
    }

    @Test
    @DisplayName("Le type est demandé au moteur par son identifiant : c'est la cible du circuit réservé")
    void typeDemandeAuMoteurParSonIdentifiant() {
        UUID idDuType = UUID.fromString("dddddddd-0000-4000-8000-000000000004");
        circuitRenduParLeMoteur(CIRCUIT_PROCEDURES);

        service.circuitDuDepot(type("PRO", "Procédure", idDuType), null);

        // C'est par cet identifiant qu'un circuit se réserve à ce type. Passer le code ('PRO') ferait
        // retomber tous les types sur le circuit par défaut.
        verify(workflowClient).circuitAOuvrir("DOCUMENT", idDuType.toString());
    }

    @Test
    @DisplayName("Le circuit rendu par le moteur est celui qu'on ouvre, sans second choix ici")
    void circuitRenduParLeMoteur_estCeluiQuOnOuvre() {
        circuitRenduParLeMoteur(CIRCUIT_PROCEDURES);

        // La règle — circuit réservé, puis circuit par défaut — est appliquée chez le moteur. La
        // rejouer ici l'aurait dédoublée, et les deux auraient fini par ne plus dire la même chose.
        assertThat(service.circuitDuDepot(type("PRO", "Procédure", TYPE_PRO), null))
                .isEqualTo(CIRCUIT_PROCEDURES);
    }

    @Test
    @DisplayName("Deux types distincts posent deux questions distinctes au moteur")
    void deuxTypes_deuxQuestionsDistinctes() {
        circuitRenduParLeMoteur(CIRCUIT_LIVRE);

        service.circuitDuDepot(type("PRO", "Procédure", TYPE_PRO), null);
        service.circuitDuDepot(type("ENR", "Enregistrement", TYPE_ENR), null);

        verify(workflowClient).circuitAOuvrir("DOCUMENT", TYPE_PRO.toString());
        verify(workflowClient).circuitAOuvrir("DOCUMENT", TYPE_ENR.toString());
    }

    @Test
    @DisplayName("Un circuit imposé par l'appelant prime, et le moteur n'est pas interrogé")
    void circuitImpose_prime() {
        UUID impose = UUID.randomUUID();

        assertThat(service.circuitDuDepot(type("PRO", "Procédure", TYPE_PRO), impose))
                .isEqualTo(impose);
        verify(workflowClient, never()).circuitAOuvrir(anyString(), any());
    }

    @Test
    @DisplayName("Ni circuit réservé ni circuit par défaut : le dépôt est refusé et expliqué")
    void aucunCircuit_depotRefuse() {
        // Ce que rend workflow-service quand aucun circuit DOCUMENT n'est actif : un 404 métier.
        when(workflowClient.circuitAOuvrir(eq("DOCUMENT"), any()))
                .thenThrow(new RuntimeException("Aucun circuit n'est réservé à cette catégorie, et la "
                        + "famille « DOCUMENT » n'a pas de circuit par défaut actif."));

        assertThatThrownBy(() -> service.circuitDuDepot(type("PRO", "Procédure", TYPE_PRO), null))
                .isInstanceOf(ResponseStatusException.class)
                // Le message nomme le type fautif : c'est par lui que l'administrateur corrige.
                .hasMessageContaining("Procédure");
    }

    @Test
    @DisplayName("Le service de circuits injoignable ne remonte pas une erreur technique")
    void serviceDeCircuitsInjoignable_refusExplicite() {
        when(workflowClient.circuitAOuvrir(eq("DOCUMENT"), any()))
                .thenThrow(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> service.circuitDuDepot(type("ENR", "Enregistrement", TYPE_ENR), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucun circuit de validation");
    }
}
