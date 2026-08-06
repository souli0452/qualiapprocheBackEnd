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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un circuit par type de document, et un repli pour les types qui n'en désignent aucun.
 *
 * <p>C'est la configuration que l'on attend d'un système documentaire : une procédure ne se valide
 * pas comme un enregistrement. Elle repose sur deux mécanismes distincts qu'il est facile de
 * confondre — le circuit <b>désigné par le type</b> ({@code QmsDocumentType.workflowId}), et le
 * circuit de <b>repli</b> de la famille DOCUMENT, celui que le serveur livre au premier
 * démarrage.</p>
 *
 * <p>Ces tests fixent lequel s'applique, et surtout qu'un circuit ajouté pour un type ne déplace pas
 * le repli des autres : plusieurs circuits documentaires actifs coexistent nécessairement, puisqu'un
 * circuit désactivé n'est pas ouvrable.</p>
 */
class UnCircuitParTypeDeDocumentTest {

    /** Circuit livré au premier démarrage : le repli de tous les types qui ne désignent rien. */
    private static final UUID CIRCUIT_LIVRE = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    /** Circuits ajoutés ensuite, chacun attribué à un type. */
    private static final UUID CIRCUIT_PROCEDURES = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000002");
    private static final UUID CIRCUIT_ENREGISTREMENTS = UUID.fromString("cccccccc-0000-4000-8000-000000000003");

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

    private QmsDocumentType type(String code, String libelle, UUID circuit) {
        return QmsDocumentType.builder()
                .code(code)
                .libelle(libelle)
                .folderName(libelle)
                .workflowId(circuit)
                .build();
    }

    /** Le circuit de repli tel que le rend workflow-service : le plus ancien circuit ouvrable. */
    private void circuitDeRepli(UUID id) {
        WorkflowSummaryDto repli = new WorkflowSummaryDto();
        repli.setId(id);
        when(workflowClient.getActiveWorkflowByType("DOCUMENT")).thenReturn(repli);
    }

    @Test
    @DisplayName("Chaque type suit le circuit qu'il désigne, et deux types ne suivent pas le même")
    void chaqueTypeSuitSonCircuit() {
        circuitDeRepli(CIRCUIT_LIVRE);

        UUID pourUneProcedure = service.circuitDuDepot(
                type("PRO", "Procédure", CIRCUIT_PROCEDURES), null);
        UUID pourUnEnregistrement = service.circuitDuDepot(
                type("ENR", "Enregistrement", CIRCUIT_ENREGISTREMENTS), null);

        assertThat(pourUneProcedure).isEqualTo(CIRCUIT_PROCEDURES);
        assertThat(pourUnEnregistrement).isEqualTo(CIRCUIT_ENREGISTREMENTS);
        // Ni l'un ni l'autre ne retombe sur le circuit livré, alors qu'il reste le repli de la famille.
        assertThat(pourUneProcedure).isNotEqualTo(CIRCUIT_LIVRE);
        assertThat(pourUnEnregistrement).isNotEqualTo(CIRCUIT_LIVRE);
    }

    @Test
    @DisplayName("Un type qui ne désigne aucun circuit prend celui livré au démarrage")
    void typeSansCircuit_prendLeCircuitLivre() {
        circuitDeRepli(CIRCUIT_LIVRE);

        assertThat(service.circuitDuDepot(type("INS", "Instruction de travail", null), null))
                .isEqualTo(CIRCUIT_LIVRE);
    }

    @Test
    @DisplayName("Attribuer un circuit à un type ne déplace pas le repli des autres types")
    void circuitAjoutePourUnType_neDeplacePasLeRepli() {
        // Trois circuits documentaires actifs coexistent : c'est la situation normale dès qu'on en
        // attribue par type, un circuit désactivé n'étant pas ouvrable. Le repli reste le plus
        // ancien, et workflow-service le désigne sans ambiguïté depuis qu'il trie par ancienneté.
        circuitDeRepli(CIRCUIT_LIVRE);

        assertThat(service.circuitDuDepot(type("PRO", "Procédure", CIRCUIT_PROCEDURES), null))
                .isEqualTo(CIRCUIT_PROCEDURES);
        assertThat(service.circuitDuDepot(type("FOR", "Formulaire", null), null))
                .isEqualTo(CIRCUIT_LIVRE);
        assertThat(service.circuitDuDepot(type("NOR", "Norme", null), null))
                .isEqualTo(CIRCUIT_LIVRE);
    }

    @Test
    @DisplayName("Le circuit du type est pris sans interroger le repli : une seule règle s'applique")
    void circuitDuType_nInterrogePasLeRepli() {
        service.circuitDuDepot(type("PRO", "Procédure", CIRCUIT_PROCEDURES), null);

        // Un aller-retour inutile, mais surtout : si le repli était consulté puis préféré, un
        // circuit attribué à un type deviendrait sans effet le jour où l'on activerait un autre
        // circuit documentaire.
        verify(workflowClient, never()).getActiveWorkflowByType("DOCUMENT");
    }

    @Test
    @DisplayName("Un circuit imposé par l'appelant prime sur celui du type")
    void circuitImpose_primeSurLeType() {
        UUID impose = UUID.randomUUID();

        assertThat(service.circuitDuDepot(type("PRO", "Procédure", CIRCUIT_PROCEDURES), impose))
                .isEqualTo(impose);
    }

    @Test
    @DisplayName("Ni circuit sur le type ni repli dans la famille : le dépôt est refusé et expliqué")
    void aucunCircuit_depotRefuse() {
        // Ce que rend workflow-service quand aucun circuit DOCUMENT n'est actif : un 404 métier.
        when(workflowClient.getActiveWorkflowByType("DOCUMENT"))
                .thenThrow(new RuntimeException("Aucun workflow actif n'est configuré pour le type 'DOCUMENT'."));

        assertThatThrownBy(() -> service.circuitDuDepot(type("PRO", "Procédure", null), null))
                .isInstanceOf(ResponseStatusException.class)
                // Le message nomme le type fautif : c'est par lui que l'administrateur corrige.
                .hasMessageContaining("Procédure");
    }

    @Test
    @DisplayName("Le service de circuits injoignable ne remonte pas une erreur technique")
    void serviceDeCircuitsInjoignable_refusExplicite() {
        when(workflowClient.getActiveWorkflowByType("DOCUMENT"))
                .thenThrow(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> service.circuitDuDepot(type("ENR", "Enregistrement", null), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucun circuit de validation");
    }
}
