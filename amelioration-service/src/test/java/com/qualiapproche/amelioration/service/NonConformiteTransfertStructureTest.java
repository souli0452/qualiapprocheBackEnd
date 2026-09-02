package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.repository.ActionRepository;
import com.qualiapproche.amelioration.repository.EfficaciteRepository;
import com.qualiapproche.amelioration.repository.NiveauNonConformiteRepository;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.repository.SourceNonConformiteRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.NonConformiteServiceImpl;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.service.SendMailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.qualiapproche.common.dto.StructureDto;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Passage d'une non-conformité d'une structure à une autre.
 *
 * <p>Le transfert s'écrivait par une mise à jour ordinaire du dossier : rien ne disait qui l'avait
 * décidé ni quand, et le champ pouvait être défait par n'importe quel enregistrement suivant. Il
 * arrive désormais du moteur, avec le reste de la décision — ces tests fixent ce qu'il en advient
 * sur la non-conformité.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonConformiteTransfertStructureTest {

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private NonConformiteMapper nonConformiteMapper;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private SourceNonConformiteRepository sourceNonConformiteRepository;
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

    @InjectMocks private NonConformiteServiceImpl service;

    private final UUID ncId = UUID.randomUUID();

    private NonConformite nonConformiteConfieeA(String structureId, String libelle) {
        NonConformite nc = new NonConformite();
        nc.setId(ncId);
        nc.setOrigineId(structureId);
        nc.setOrigineService(libelle);
        when(nonConformiteRepository.findById(ncId)).thenReturn(Optional.of(nc));
        return nc;
    }

    private void decision(Map<String, String> champs) {
        service.updateWorkflowState(ncId, "EN_COURS", "Réception", "RECEPTION", champs);
    }

    private static final UUID STRUCTURE_CIBLE = UUID.randomUUID();

    /** Le circuit ne transmet qu'un identifiant : le nom se lit au référentiel. */
    private void referentielConnait(UUID id, String libelleLong, String libelleCourt) {
        StructureDto structure = new StructureDto();
        structure.setLibelleLong(libelleLong);
        structure.setLibelleCourt(libelleCourt);
        when(referentielClient.getStructureById(id)).thenReturn(structure);
    }

    @Test
    @DisplayName("La structure désignée à l'étape devient celle à qui le dossier est confié")
    void structureDesignee_confieeLeDossier() {
        NonConformite nc = nonConformiteConfieeA("structure-emettrice", "Direction des systèmes");
        referentielConnait(STRUCTURE_CIBLE, "Direction de la qualité", "DQ");

        decision(Map.of("structureDestinataireId", STRUCTURE_CIBLE.toString()));

        assertThat(nc.getOrigineId()).isEqualTo(STRUCTURE_CIBLE.toString());
        // Le nom n'est pas ressaisi par l'utilisateur : il vient du référentiel, seule source qui
        // en réponde.
        assertThat(nc.getOrigineService()).isEqualTo("Direction de la qualité");
        assertThat(nc.getOrigineServiceLibelleCourt()).isEqualTo("DQ");
    }

    @Test
    @DisplayName("Un référentiel injoignable n'empêche pas le transfert, il le laisse sans nom")
    void referentielIndisponible_transfertQuandMeme() {
        NonConformite nc = nonConformiteConfieeA("structure-emettrice", "Direction des systèmes");
        when(referentielClient.getStructureById(STRUCTURE_CIBLE))
                .thenThrow(new RuntimeException("référentiel injoignable"));

        decision(Map.of("structureDestinataireId", STRUCTURE_CIBLE.toString()));

        // L'affectation est la décision ; la nommer n'en est que la présentation. La refuser aurait
        // fait dépendre une décision de circuit de la disponibilité d'un autre service.
        assertThat(nc.getOrigineId()).isEqualTo(STRUCTURE_CIBLE.toString());
        assertThat(nc.getOrigineService()).isEqualTo("Direction des systèmes");
    }

    @Test
    @DisplayName("Une décision qui ne désigne aucune structure ne déplace pas le dossier")
    void aucuneDesignation_dossierImmobile() {
        NonConformite nc = nonConformiteConfieeA("structure-en-place", "Direction en place");

        decision(Map.of("pertinanceRs", "Oui"));

        assertThat(nc.getOrigineId()).isEqualTo("structure-en-place");
        assertThat(nc.getOrigineService()).isEqualTo("Direction en place");
    }

    @Test
    @DisplayName("Un champ vide n'est pas un transfert vers nulle part")
    void designationVide_dossierImmobile() {
        NonConformite nc = nonConformiteConfieeA("structure-en-place", "Direction en place");

        decision(Map.of("structureDestinataireId", "   "));

        assertThat(nc.getOrigineId()).isEqualTo("structure-en-place");
    }

    @Test
    @DisplayName("Redésigner la structure déjà en place ne change rien")
    void memeStructure_sansEffet() {
        NonConformite nc = nonConformiteConfieeA("structure-en-place", "Direction en place");

        decision(Map.of("structureDestinataireId", "structure-en-place"));

        // Aucun appel au référentiel : rien n'a changé, il n'y a rien à renommer.
        assertThat(nc.getOrigineService()).isEqualTo("Direction en place");
    }
}
