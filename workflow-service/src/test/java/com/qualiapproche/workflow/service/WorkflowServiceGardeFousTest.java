package com.qualiapproche.workflow.service;

import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.dto.WorkflowDto;
import com.qualiapproche.workflow.dto.WorkflowStepDto;
import com.qualiapproche.workflow.event.CatalogueWorkflowModifieEvent;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import com.qualiapproche.workflow.repository.WorkflowFieldValueRepository;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import com.qualiapproche.workflow.repository.WorkflowStepFieldRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowTransitionRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.qualiapproche.workflow.dto.WorkflowTransitionDto;

/**
 * Garde-fous d'ouverture et de suppression d'un circuit.
 *
 * <p>Chacun des cas couverts ici passait sans erreur avant correction, en laissant derrière lui
 * une donnée incohérente : deux circuits concurrents sur un même dossier, ou des instances en
 * cours rattachées à un circuit disparu.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceGardeFousTest {

    @Mock private IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> moteur;
    @Mock private ValidationHistoryRepository historyRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WorkflowRepository workflowRepository;
    @Mock private WorkflowValidationInstanceRepository validationInstanceRepository;
    @Mock private WorkflowStepFieldRepository stepFieldRepository;
    @Mock private WorkflowFieldValueRepository fieldValueRepository;
    @Mock private WorkflowTransitionRepository transitionRepository;
    @Mock private WorkflowStepRepository stepRepository;

    private WorkflowService service;

    private final UUID workflowId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkflowService(moteur, historyRepository, eventPublisher, workflowRepository,
                validationInstanceRepository, stepFieldRepository, fieldValueRepository,
                transitionRepository, stepRepository,
                org.mockito.Mockito.mock(StructureUtilisateurService.class),
                // Pas de proxy hors contexte Spring : voir WorkflowService#self().
                null);
    }

    private Workflow circuit(boolean actif, String resourceType, int nbEtapes) {
        Workflow aWorkflow = Workflow.builder()
                .nom("Validation standard")
                .resourceType(resourceType)
                .actif(actif)
                .build();
        aWorkflow.setId(workflowId);

        List<WorkflowStep> aEtapes = new ArrayList<>();
        for (long i = 1; i <= nbEtapes; i++) {
            WorkflowStep aEtape = new WorkflowStep();
            aEtape.setId(i);
            aEtape.setCode("ETAPE_" + i);
            aEtape.setNomEtape("Étape " + i);
            aEtape.setWorkflow(aWorkflow);
            aEtapes.add(aEtape);
        }
        aWorkflow.setSteps(aEtapes);
        return aWorkflow;
    }

    private WorkflowValidationInstance instanceEnCours(String workflowCode) {
        return WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(resourceId.toString())
                .resourceType("DOCUMENT")
                .workflowCode(workflowCode)
                .etatCode("1")
                .status(ValidationStatus.EN_COURS)
                .build();
    }

    // ------------------------------------------------------------------ circuit à ouvrir

    /** Circuit d'une famille, réservé ou non à une catégorie, avec son ancienneté. */
    private Workflow circuitCible(String nom, String cible, boolean actif, int joursDAge) {
        Workflow aWorkflow = circuit(actif, "DOCUMENT", 2);
        aWorkflow.setId(UUID.randomUUID());
        aWorkflow.setNom(nom);
        aWorkflow.setCibleId(cible);
        aWorkflow.setCreatedAt(java.time.LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(joursDAge));
        return aWorkflow;
    }

    @Test
    @DisplayName("Le circuit réservé à la catégorie l'emporte sur le circuit par défaut")
    void circuitReserve_lEmporteSurLeDefaut() {
        Workflow aDefaut = circuitCible("Validation documentaire standard", null, true, 0);
        Workflow aDesProcedures = circuitCible("Validation des procédures", "type-pro", true, 30);
        when(workflowRepository.findByResourceType("DOCUMENT"))
                .thenReturn(List.of(aDefaut, aDesProcedures));

        assertThat(service.circuitPourFamilleEtCible("DOCUMENT", "type-pro").getNom())
                .isEqualTo("Validation des procédures");
    }

    @Test
    @DisplayName("Une catégorie sans circuit réservé prend le circuit par défaut de la famille")
    void categorieSansCircuit_prendLeDefaut() {
        when(workflowRepository.findByResourceType("DOCUMENT")).thenReturn(List.of(
                circuitCible("Validation documentaire standard", null, true, 0),
                circuitCible("Validation des procédures", "type-pro", true, 30)));

        // Le circuit des procédures ne doit pas servir un enregistrement : il est réservé.
        assertThat(service.circuitPourFamilleEtCible("DOCUMENT", "type-enr").getNom())
                .isEqualTo("Validation documentaire standard");
    }

    @Test
    @DisplayName("Un circuit réservé mais désactivé ne sert pas : la catégorie retombe sur le défaut")
    void circuitReserveDesactive_retombeSurLeDefaut() {
        when(workflowRepository.findByResourceType("DOCUMENT")).thenReturn(List.of(
                circuitCible("Validation documentaire standard", null, true, 0),
                circuitCible("Validation des procédures", "type-pro", false, 30)));

        // Le moteur refuse d'ouvrir un circuit désactivé : le proposer ici n'aurait produit qu'un
        // refus plus tard, au dépôt.
        assertThat(service.circuitPourFamilleEtCible("DOCUMENT", "type-pro").getNom())
                .isEqualTo("Validation documentaire standard");
    }

    @Test
    @DisplayName("Deux circuits par défaut concurrents : le plus ancien tranche, de façon reproductible")
    void deuxDefautsConcurrents_lePlusAncienTranche() {
        // Situation que la contrainte de base interdira ; d'ici là, le choix ne doit pas dépendre de
        // l'ordre dans lequel la base rend les lignes.
        when(workflowRepository.findByResourceType("DOCUMENT")).thenReturn(List.of(
                circuitCible("Ajouté ensuite", null, true, 30),
                circuitCible("Livré au démarrage", null, true, 0)));

        assertThat(service.circuitPourFamilleEtCible("DOCUMENT", "type-pro").getNom())
                .isEqualTo("Livré au démarrage");
    }

    @Test
    @DisplayName("Ni circuit réservé ni circuit par défaut : refus en 404 explicite")
    void aucunCircuit_refuseEn404() {
        when(workflowRepository.findByResourceType("DOCUMENT")).thenReturn(List.of(
                circuitCible("Validation des procédures", "type-pro", true, 0)));

        assertThatThrownBy(() -> service.circuitPourFamilleEtCible("DOCUMENT", "type-enr"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pas de circuit par défaut");
    }

    @Test
    @DisplayName("Le circuit par défaut d'une famille se demande sans cible")
    void circuitParDefaut_seDemandeSansCible() {
        when(workflowRepository.findByResourceType("NON_CONFORMITE")).thenReturn(List.of(
                circuitCible("Traitement des non-conformités", null, true, 0)));

        assertThat(service.getActiveWorkflowByType("NON_CONFORMITE").getNom())
                .isEqualTo("Traitement des non-conformités");
    }

    // ------------------------------------------------------------------ ouverture

    @Test
    @DisplayName("Ouvrir deux fois le même circuit rend l'instance déjà en cours, sans la dupliquer")
    void ouvertureRejouee_memeCircuit_rendLInstanceExistante() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 2)));
        WorkflowValidationInstance aExistante = instanceEnCours(workflowId.toString());
        when(validationInstanceRepository
                .findTopByResourceIdAndStatusOrderByStartedAtDesc(resourceId.toString(), ValidationStatus.EN_COURS))
                .thenReturn(Optional.of(aExistante));

        WorkflowInstanceDto aDto = service.initiateWorkflow(resourceId, "DOCUMENT", workflowId);

        assertThat(aDto.getInstanceId()).isEqualTo(aExistante.getId());
        verify(validationInstanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ouvrir un autre circuit sur un dossier déjà engagé est refusé en 409")
    void ouverture_autreCircuitEnCours_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 2)));
        when(validationInstanceRepository
                .findTopByResourceIdAndStatusOrderByStartedAtDesc(resourceId.toString(), ValidationStatus.EN_COURS))
                .thenReturn(Optional.of(instanceEnCours(UUID.randomUUID().toString())));

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà en cours")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(validationInstanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un circuit désactivé ne peut plus être ouvert")
    void ouverture_circuitDesactive_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(false, "DOCUMENT", 2)));

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    @DisplayName("Un circuit sans étape est refusé plutôt que de produire une instance sans état")
    void ouverture_circuitSansEtape_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 0)));

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("aucune étape");
    }

    @Test
    @DisplayName("Un circuit prévu pour un autre type de ressource est refusé en 400")
    void ouverture_typeDeRessourceIncoherent_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "NON_CONFORMITE", 2)));

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Un circuit introuvable donne un 404 et non une erreur serveur")
    void ouverture_circuitIntrouvable_404() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------ suppression

    @Test
    @DisplayName("Supprimer un circuit portant des dossiers en cours est refusé en 409")
    void suppression_dossiersEnCours_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 2)));
        when(validationInstanceRepository.existsByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.deleteWorkflow(workflowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en cours")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(workflowRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Supprimer un circuit sans dossier en cours reste possible")
    void suppression_sansDossierEnCours_autorisee() {
        Workflow aCircuit = circuit(true, "DOCUMENT", 2);
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(aCircuit));
        when(validationInstanceRepository.existsByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(false);

        service.deleteWorkflow(workflowId);

        verify(workflowRepository).delete(aCircuit);
    }

    @Test
    @DisplayName("Le rechargement du catalogue est différé après commit, et non joué dans la transaction")
    void modificationDuCatalogue_rechargementDiffereApresCommit() throws Exception {
        Workflow aCircuit = circuit(true, "DOCUMENT", 2);
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(aCircuit));
        when(validationInstanceRepository.existsByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(false);

        service.deleteWorkflow(workflowId);

        // Recharger dans la transaction exposait le moteur à des données non committées : une
        // annulation ultérieure lui laissait un catalogue décrivant des circuits inexistants.
        verify(moteur, never()).init();
        verify(eventPublisher).publishEvent(any(CatalogueWorkflowModifieEvent.class));
    }

    @Test
    @DisplayName("Supprimer un circuit introuvable donne un 404")
    void suppression_circuitIntrouvable_404() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteWorkflow(workflowId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Aucun workflow actif pour un type donne un 404 explicite")
    void workflowActif_absent_404() {
        when(workflowRepository.findByResourceTypeAndActifTrue(anyString())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getActiveWorkflowByType("DOCUMENT"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------ configuration

    private WorkflowDto dtoCircuit(String resourceType) {
        return WorkflowDto.builder()
                .nom("wok")
                .resourceType(resourceType)
                .steps(new ArrayList<>(List.of(
                        WorkflowStepDto.builder().nomEtape("Rédaction").stepOrder(1).build())))
                .build();
    }

    /** Circuit proposé à l'enregistrement, avec sa famille et sa cible. */
    private WorkflowDto dtoCircuit(String resourceType, String cible) {
        WorkflowDto dto = dtoCircuit(resourceType);
        dto.setCibleId(cible);
        return dto;
    }

    @Test
    @DisplayName("Un second circuit par défaut pour une même famille est refusé, en nommant le premier")
    void creation_secondCircuitParDefaut_refusee() {
        Workflow aDejaLa = circuit(true, "DOCUMENT", 1);
        aDejaLa.setNom("Validation documentaire standard");
        when(workflowRepository.findByResourceType("DOCUMENT")).thenReturn(List.of(aDejaLa));

        // Sans ce refus, deux circuits sans cible se disputeraient les dossiers qui n'en désignent
        // aucun, et le choix se jouerait sur l'ordre de la base.
        assertThatThrownBy(() -> service.createWorkflow(dtoCircuit("DOCUMENT", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Validation documentaire standard")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deux circuits réservés à la même catégorie sont refusés")
    void creation_deuxCircuitsMemeCible_refusee() {
        Workflow aDejaLa = circuit(true, "DOCUMENT", 1);
        aDejaLa.setNom("Validation des procédures");
        aDejaLa.setCibleId("type-pro");
        when(workflowRepository.findByResourceType("DOCUMENT")).thenReturn(List.of(aDejaLa));

        assertThatThrownBy(() -> service.createWorkflow(dtoCircuit("DOCUMENT", "type-pro")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà réservé")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Un circuit réservé à une autre catégorie est accepté à côté du circuit par défaut")
    void creation_cibleDistincte_acceptee() {
        Workflow aDefaut = circuit(true, "DOCUMENT", 1);
        aDefaut.setNom("Validation documentaire standard");
        when(workflowRepository.findByResourceType("DOCUMENT")).thenReturn(List.of(aDefaut));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.createWorkflow(dtoCircuit("DOCUMENT", "type-enr")).getCibleId())
                .isEqualTo("type-enr");
    }

    @Test
    @DisplayName("Une cible vide vaut « aucune cible » : elle n'est pas persistée comme catégorie")
    void creation_cibleVide_vautAucuneCible() {
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        // Le champ effacé dans l'éditeur arrive en chaîne vide ; le circuit doit redevenir celui de
        // toute la famille, et non être réservé à une catégorie nommée « ».
        assertThat(service.createWorkflow(dtoCircuit("DOCUMENT", "   ")).getCibleId()).isNull();
    }

    @Test
    @DisplayName("Un circuit désactivé ne dispute pas sa place : il n'entre dans aucune résolution")
    void creation_circuitDesactive_nEntrePasEnConflit() {
        Workflow aDefaut = circuit(true, "DOCUMENT", 1);
        aDefaut.setNom("Validation documentaire standard");
        when(workflowRepository.findByResourceType("DOCUMENT")).thenReturn(List.of(aDefaut));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowDto aPropose = dtoCircuit("DOCUMENT", null);
        aPropose.setActif(false);

        // Garder l'ancien circuit d'une catégorie à côté du nouveau doit rester possible : il porte
        // encore des dossiers en cours, et l'exiger supprimé les rendrait inexploitables.
        assertThat(service.createWorkflow(aPropose)).isNotNull();
    }

    @Test
    @DisplayName("L'activation est reprise à la modification : l'interrupteur avait perdu tout effet")
    void modification_activationReprise() {
        Workflow existant = circuit(true, "DOCUMENT", 1);
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(existant));
        when(workflowRepository.findByResourceType("DOCUMENT")).thenReturn(List.of(existant));

        WorkflowDto aPropose = dtoCircuit("DOCUMENT", null);
        aPropose.setActif(false);
        service.updateWorkflow(workflowId, aPropose);

        // `updateWorkflow` ne reportait ni l'activation ni la cible : un circuit ne pouvait plus être
        // retiré du service une fois créé, et l'éditeur donnait le change.
        assertThat(existant.isActif()).isFalse();
    }

    @Test
    @DisplayName("Créer un circuit sur un code de type documentaire est refusé à la configuration")
    void creation_typeDeRessourceInconnu_refusee() {
        // 'PRO' est un type de document, pas une famille de ressource : le circuit était accepté,
        // puis rejeté à l'ouverture — après le dépôt du fichier, et pour l'utilisateur suivant.
        assertThatThrownBy(() -> service.createWorkflow(dtoCircuit("PRO")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DOCUMENT")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("La casse et les espaces du type de ressource sont normalisés, pas refusés")
    void creation_typeDeRessourceNormalise() {
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowDto aCree = service.createWorkflow(dtoCircuit("  document "));

        assertThat(aCree.getResourceType()).isEqualTo("DOCUMENT");
    }

    @Test
    @DisplayName("Un type de ressource absent est refusé plutôt que persisté vide")
    void creation_typeDeRessourceAbsent_refusee() {
        assertThatThrownBy(() -> service.createWorkflow(dtoCircuit(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modifier un circuit vers un type de ressource inconnu est refusé de même")
    void modification_typeDeRessourceInconnu_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 1)));

        assertThatThrownBy(() -> service.updateWorkflow(workflowId, dtoCircuit("PRO")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(workflowRepository, never()).save(any());
    }

    // ------------------------------------------------------- apparence des actions

    /** Circuit d'une étape portant une transition dont on fixe l'apparence. */
    private WorkflowDto dtoAvecApparence(String icone, String severite) {
        return WorkflowDto.builder()
                .nom("wok")
                .resourceType("DOCUMENT")
                .steps(new ArrayList<>(List.of(
                        WorkflowStepDto.builder().nomEtape("Rédaction").stepOrder(1)
                                .transitions(new ArrayList<>(List.of(
                                        WorkflowTransitionDto.builder()
                                                .decision("APPROUVE")
                                                .label("Soumettre")
                                                .icon(icone)
                                                .severity(severite)
                                                .terminal(true)
                                                .build())))
                                .build())))
                .build();
    }

    @Test
    @DisplayName("Libellé, icône et couleur du bouton sont enregistrés sur la transition")
    void configuration_apparenceEnregistree() {
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowDto aCree = service.createWorkflow(dtoAvecApparence("pi pi-send", "info"));

        assertThat(aCree.getSteps().get(0).getTransitions()).singleElement().satisfies(t -> {
            assertThat(t.getLabel()).isEqualTo("Soumettre");
            assertThat(t.getIcon()).isEqualTo("pi pi-send");
            assertThat(t.getSeverity()).isEqualTo("info");
        });
    }

    @Test
    @DisplayName("La couleur est lue quelle qu'en soit la casse, et « warning » vaut « warn »")
    void configuration_couleurNormalisee() {
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        // PrimeNG a renommé cette sévérité entre ses versions 17 et 18 : accepter les deux
        // graphies évite que l'écran de configuration ait à suivre la version du thème.
        assertThat(service.createWorkflow(dtoAvecApparence(null, "WARNING"))
                .getSteps().get(0).getTransitions().get(0).getSeverity()).isEqualTo("warn");
        assertThat(service.createWorkflow(dtoAvecApparence(null, " Danger "))
                .getSteps().get(0).getTransitions().get(0).getSeverity()).isEqualTo("danger");
    }

    @Test
    @DisplayName("Une couleur inconnue est refusée en 400, la liste des jetons admis à l'appui")
    void configuration_couleurInconnue_refusee() {
        assertThatThrownBy(() -> service.createWorkflow(dtoAvecApparence(null, "sucess")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sucess")
                .hasMessageContaining("success")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une couleur absente reste vide en configuration : le défaut se lit à l'exécution")
    void configuration_couleurAbsente_nonInventee() {
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowDto aCree = service.createWorkflow(dtoAvecApparence(null, null));

        assertThat(aCree.getSteps().get(0).getTransitions()).singleElement().satisfies(t -> {
            assertThat(t.getIcon()).isNull();
            assertThat(t.getSeverity()).isNull();
        });
    }
}
