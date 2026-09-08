package com.qualiapproche.amelioration.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.ActionRepository;
import com.qualiapproche.amelioration.repository.EfficaciteRepository;
import com.qualiapproche.amelioration.repository.NiveauNonConformiteRepository;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.NonConformiteServiceImpl;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.dto.NcDashboardDto;
import com.qualiapproche.common.enumeration.AvancementCircuit;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.common.utils.StatutEnum;

/**
 * Les chiffres du tableau de bord des non-conformités.
 *
 * <p>Ce qui s'y joue : l'avancement vient du <b>moteur</b> et non d'un état inscrit sur le
 * dossier. Compter les dossiers clos en rapprochant le libellé de l'étape, ou les brouillons en
 * cherchant {@code Status.DRAFT}, reviendrait à tenir dans ce module une seconde table de règles —
 * fausse dès qu'une étape est ajoutée au circuit, muette pour un second circuit servant la même
 * famille de dossiers.</p>
 *
 * <p>Une seule exception, et elle ne contredit pas la règle : le dossier dont le moteur ne sait
 * rien — ceux d'avant la bascule — est rattrapé sur {@code Etat.CLOTURE}, qui n'est pas une règle
 * concurrente mais le code d'état que le moteur avait lui-même posé sur l'étape atteinte. Dès que
 * le moteur répond, il tranche seul.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TableauDeBordNcTest {

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private NonConformiteMapper nonConformiteMapper;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private TypeNonConformiteRepository typeNonConformiteRepository;
    @Mock private ReferentielClient referentielClient;
    @Mock private EfficaciteRepository efficaciteRepository;
    @Mock private ActionRepository actionRepository;
    @Mock private NiveauNonConformiteRepository niveauNonConformiteRepository;
    @Mock private PieceJointeStockageService fichierService;
    @Mock private NonConformiteFichierService ncFichierService;
    @Mock private SendMailService sendMailService;
    @Mock private PlanActionRepository planActionRepository;
    @Mock private WorkflowClient workflowClient;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;
    @Mock private PermissionChecker permissionChecker;

    @InjectMocks private NonConformiteServiceImpl service;

    private static NonConformite dossier(UUID id) {
        NonConformite nc = new NonConformite();
        nc.setId(id);
        return nc;
    }

    private static PlanAction action(UUID dossier, LocalDate echeance, LocalDate realisation,
                                     StatutEnum statut) {
        PlanAction plan = new PlanAction();
        plan.setNonConformeId(dossier);
        plan.setDateEcheance(echeance);
        plan.setDateTraitement(realisation);
        plan.setStatus(statut);
        return plan;
    }

    private void dossiers(Map<UUID, AvancementCircuit> avancements) {
        when(nonConformiteRepository.findAll())
                .thenReturn(avancements.keySet().stream().map(TableauDeBordNcTest::dossier).toList());
        when(workflowClient.avancementDesRessources(anyList())).thenReturn(avancements);
    }

    /**
     * Un périmètre où le moteur ne connaît qu'une partie des dossiers.
     *
     * @param tous        les dossiers du périmètre, avec l'état qu'ils portent
     * @param avancements ce que le moteur sait dire, qui peut n'en couvrir aucun
     */
    private void dossiers(List<NonConformite> tous, Map<UUID, AvancementCircuit> avancements) {
        when(nonConformiteRepository.findAll()).thenReturn(tous);
        when(workflowClient.avancementDesRessources(anyList())).thenReturn(avancements);
    }

    private static NonConformite dossier(UUID id, Etat etat) {
        NonConformite nc = dossier(id);
        nc.setEtatTraitement(etat);
        return nc;
    }

    private void actions(PlanAction... plans) {
        when(planActionRepository.findByNonConformeIdIn(anyCollection())).thenReturn(List.of(plans));
    }

    @Test
    @DisplayName("L'avancement est celui que le moteur établit, non l'état inscrit sur le dossier")
    void avancement_vientDuMoteur() {
        UUID clos = UUID.randomUUID();
        UUID enCours = UUID.randomUUID();
        UUID jamaisSoumis = UUID.randomUUID();
        dossiers(Map.of(clos, AvancementCircuit.TERMINE,
                enCours, AvancementCircuit.EN_COURS,
                jamaisSoumis, AvancementCircuit.NON_ENGAGE));
        actions();

        NcDashboardDto tableau = service.getDashboardRQ();

        assertThat(tableau.getTotal()).isEqualTo(3);
        assertThat(tableau.getTotalNC()).isEqualTo(3);
        assertThat(tableau.getEnCours()).isEqualTo(1);
        assertThat(tableau.getCloturees()).isEqualTo(1);
        // Un dossier jamais soumis n'est ni en cours ni clos : la somme des deux est inférieure
        // au total, et c'est voulu.
        assertThat(tableau.getTauxResolution()).isEqualTo(33.3);
    }

    @Test
    @DisplayName("Le compte par étape est celui du moteur, sans qu'aucune étape soit nommée ici")
    void parEtape_vientDuMoteur() {
        // Les libellés viennent du circuit : le module n'en connaît aucun, et une étape ajoutée
        // à l'éditeur apparaît dans le compte sans qu'une ligne change ici.
        when(workflowClient.mesDossiersParEtape("NON_CONFORMITE"))
                .thenReturn(Map.of("Réception", 3L, "Imputation", 5L));

        assertThat(service.mesNonConformitesParEtape())
                .containsExactlyInAnyOrderEntriesOf(Map.of("Réception", 3L, "Imputation", 5L));
    }

    @Test
    @DisplayName("Moteur muet : une carte vide, et non un écran en erreur")
    void parEtape_moteurMuet_carteVide() {
        // Le reste de l'accueil ne dépend pas de lui : un compteur absent vaut mieux qu'une page
        // qui ne s'affiche pas.
        when(workflowClient.mesDossiersParEtape("NON_CONFORMITE"))
                .thenThrow(new IllegalStateException("workflow-service injoignable"));

        assertThat(service.mesNonConformitesParEtape()).isEmpty();
    }

    @Test
    @DisplayName("Un dossier clos avant la bascule, que le moteur ignore, compte parmi les clôturés")
    void dossierDAvantLaBascule_compteParmiLesClotures() {
        UUID ancien = UUID.randomUUID();
        UUID suiviParLeMoteur = UUID.randomUUID();
        dossiers(List.of(dossier(ancien, Etat.CLOTURE), dossier(suiviParLeMoteur)),
                Map.of(suiviParLeMoteur, AvancementCircuit.EN_COURS));
        actions();

        NcDashboardDto tableau = service.getDashboardRQ();

        assertThat(tableau.getCloturees()).isEqualTo(1);
        assertThat(tableau.getEnCours()).isEqualTo(1);
        // Le moteur muet sur un dossier d'avant la bascule ne rend pas le compte incomplet : son
        // sort est connu. Sans quoi le taux se serait tu sur toute base contenant de l'historique.
        assertThat(tableau.getTauxResolution()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Le moteur l'emporte sur l'état inscrit : un dossier rouvert est en cours, pas clos")
    void moteurLEmporte_surLEtatInscrit() {
        UUID rouvert = UUID.randomUUID();
        dossiers(List.of(dossier(rouvert, Etat.CLOTURE)),
                Map.of(rouvert, AvancementCircuit.EN_COURS));
        actions();

        NcDashboardDto tableau = service.getDashboardRQ();

        assertThat(tableau.getEnCours()).isEqualTo(1);
        assertThat(tableau.getCloturees()).isZero();
    }

    @Test
    @DisplayName("Un dossier dont ni le moteur ni l'état ne disent rien laisse le taux muet")
    void dossierInconnu_laisseLeTauxMuet() {
        UUID inconnu = UUID.randomUUID();
        UUID clos = UUID.randomUUID();
        dossiers(List.of(dossier(inconnu), dossier(clos)), Map.of(clos, AvancementCircuit.TERMINE));
        actions();

        NcDashboardDto tableau = service.getDashboardRQ();

        assertThat(tableau.getCloturees()).isEqualTo(1);
        assertThat(tableau.getTauxResolution()).isNull();
    }

    @Test
    @DisplayName("Un dossier est en retard dès qu'une de ses actions est échue et non soldée")
    void enRetard_seCompteSurLesActions() {
        UUID dossier = UUID.randomUUID();
        UUID sain = UUID.randomUUID();
        dossiers(Map.of(dossier, AvancementCircuit.EN_COURS, sain, AvancementCircuit.EN_COURS));
        actions(
                action(dossier, LocalDate.now().minusDays(5), null, StatutEnum.NON_TRAITER),
                // Deux actions en retard sur le même dossier ne le comptent qu'une fois.
                action(dossier, LocalDate.now().minusDays(2), null, StatutEnum.EN_VERIFICATION),
                action(sain, LocalDate.now().plusDays(3), null, StatutEnum.NON_TRAITER));

        assertThat(service.getDashboardRQ().getEnRetard()).isEqualTo(1);
    }

    @Test
    @DisplayName("Une action échue mais soldée ne met plus son dossier en retard")
    void actionSoldee_dossierPlusEnRetard() {
        UUID dossier = UUID.randomUUID();
        dossiers(Map.of(dossier, AvancementCircuit.TERMINE));
        actions(action(dossier, LocalDate.now().minusDays(5),
                LocalDate.now().minusDays(1), StatutEnum.TRAITER));

        NcDashboardDto tableau = service.getDashboardRQ();

        // Le retard appelle une relance : une action reconnue efficace n'en appelle plus.
        assertThat(tableau.getEnRetard()).isZero();
        // Le taux, lui, garde la trace du dépassement — sinon il remonterait à mesure que l'on
        // solde les dossiers menés en retard.
        assertThat(tableau.getTauxSla()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Le taux de respect des délais écarte les actions sans échéance")
    void tauxSla_ecarteLesActionsSansEcheance() {
        UUID dossier = UUID.randomUUID();
        dossiers(Map.of(dossier, AvancementCircuit.EN_COURS));
        actions(
                // Réalisée avant son échéance : dans les temps.
                action(dossier, LocalDate.now().minusDays(10), LocalDate.now().minusDays(12), StatutEnum.TRAITER),
                // Échéance à venir, rien n'est encore dû : dans les temps.
                action(dossier, LocalDate.now().plusDays(10), null, StatutEnum.NON_TRAITER),
                // Réalisée après son échéance : hors délai.
                action(dossier, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), StatutEnum.TRAITER),
                // Aucune date saisie : écartée, plutôt que comptée à son avantage.
                action(dossier, null, null, StatutEnum.NON_TRAITER));

        assertThat(service.getDashboardRQ().getTauxSla()).isEqualTo(66.7);
    }

    @Test
    @DisplayName("Sans action ni dossier, les taux sont nuls plutôt que nuls de valeur")
    void perimetreVide_tauxAbsents() {
        dossiers(Map.of());
        actions();

        NcDashboardDto tableau = service.getDashboardRQ();

        // Zéro pour cent se lit comme une contre-performance ; l'absence de taux dit qu'il n'y a
        // rien à rapporter.
        assertThat(tableau.getTauxSla()).isNull();
        assertThat(tableau.getTauxResolution()).isNull();
    }

    @Test
    @DisplayName("Le moteur muet laisse le taux de résolution absent plutôt que faux")
    void moteurMuet_tauxAbsent() {
        UUID dossier = UUID.randomUUID();
        when(nonConformiteRepository.findAll()).thenReturn(List.of(dossier(dossier)));
        when(workflowClient.avancementDesRessources(anyList()))
                .thenThrow(new IllegalStateException("workflow-service injoignable"));
        actions();

        NcDashboardDto tableau = service.getDashboardRQ();

        // Le reste du tableau ne dépend pas du moteur et vaut mieux qu'un écran en erreur ; le
        // taux, lui, se lit comme un jugement et ne doit pas être annoncé sur un compte amputé.
        assertThat(tableau.getTotal()).isEqualTo(1);
        assertThat(tableau.getTauxResolution()).isNull();
    }
}
