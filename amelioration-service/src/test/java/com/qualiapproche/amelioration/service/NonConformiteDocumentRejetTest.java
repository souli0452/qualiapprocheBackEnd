package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PieceJointe;
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
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteServiceImpl;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Rattachement du justificatif de rejet à la non-conformité, depuis la saisie faite dans le moteur.
 *
 * <p>Le moteur ne transporte que des chaînes : le champ ne porte que la référence de l'objet
 * déposé. Le rapprochement se fait donc en base, et il est <b>restreint à la non-conformité</b> —
 * une référence est une chaîne qui circule côté client.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonConformiteDocumentRejetTest {

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

    @InjectMocks private NonConformiteServiceImpl service;

    private static final String REFERENCE = "NON_CONFORMITE/DSI/9f1c-2d3e.pdf";

    private final UUID ncId = UUID.randomUUID();

    private NonConformite nonConformiteExistante() {
        NonConformite nc = new NonConformite();
        nc.setId(ncId);
        when(nonConformiteRepository.findById(ncId)).thenReturn(Optional.of(nc));
        return nc;
    }

    @Test
    @DisplayName("La référence saisie à l'étape rattache la pièce jointe déposée")
    void referenceSaisie_rattacheLaPieceJointe() {
        NonConformite nc = nonConformiteExistante();
        PieceJointe justificatif = PieceJointe.builder()
                .nom("courrier de refus.pdf").url(REFERENCE).entityId(ncId).build();
        when(pieceJointeRepository.findByUrlAndEntityId(REFERENCE, ncId))
                .thenReturn(Optional.of(justificatif));

        service.updateWorkflowState(ncId, "REJECTED", "Validation", "VALIDATION",
                Map.of("docRejet", REFERENCE));

        assertThat(nc.getDocRejet()).isSameAs(justificatif);
    }

    @Test
    @DisplayName("Une référence appartenant à un autre dossier n'est pas rattachée, et la "
            + "transition aboutit quand même")
    void referenceEtrangere_ignoreeSansEchec() {
        NonConformite nc = nonConformiteExistante();
        when(pieceJointeRepository.findByUrlAndEntityId(REFERENCE, ncId)).thenReturn(Optional.empty());

        // La décision est déjà prise et enregistrée par le moteur : échouer ici la ferait rejouer
        // indéfiniment par le mécanisme de reprise des notifications.
        service.updateWorkflowState(ncId, "REJECTED", "Validation", "VALIDATION",
                Map.of("docRejet", REFERENCE));

        assertThat(nc.getDocRejet()).isNull();
        assertThat(nc.getWorkflowStatus()).isEqualTo("Validation");
    }

    @Test
    @DisplayName("Une approbation sans justificatif ne détache pas celui d'un rejet précédent")
    void aucuneSaisie_conserveLeJustificatifExistant() {
        NonConformite nc = nonConformiteExistante();
        PieceJointe precedent = PieceJointe.builder().url("ancienne-reference").build();
        nc.setDocRejet(precedent);

        service.updateWorkflowState(ncId, "APPROVED", "Validation RS", "VALIDATION_RS", Map.of());

        assertThat(nc.getDocRejet()).isSameAs(precedent);
    }

    @Test
    @DisplayName("Un champ vide est sans effet : il n'efface pas le justificatif en place")
    void champVide_sansEffet() {
        NonConformite nc = nonConformiteExistante();
        PieceJointe precedent = PieceJointe.builder().url("ancienne-reference").build();
        nc.setDocRejet(precedent);

        service.updateWorkflowState(ncId, "EN_COURS", "Traitement", "TRAITEMENT",
                Map.of("docRejet", "   "));

        assertThat(nc.getDocRejet()).isSameAs(precedent);
    }
}
