package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
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
import com.qualiapproche.common.enumeration.Circuit;
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
 * Le circuit de traitement retenu par le responsable qualité arrive sur le dossier.
 *
 * <p>Il ne s'agit pas d'une mention d'affichage : de ce choix dépendent les colonnes que le plan
 * d'action devra porter. En <b>correction</b>, on remet en conformité ce qui ne l'était pas sans
 * avoir à remonter à ce qui l'a produit, et la colonne « cause » disparaît ; en <b>action
 * corrective</b>, elle est le cœur du sujet. Sans cette reprise, la décision aurait été prise et
 * enregistrée par le moteur, et le module qui contrôle les plans d'action n'en aurait rien su.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CircuitDeTraitementTest {

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

    private NonConformite dossier(Circuit circuitEnPlace) {
        NonConformite nc = new NonConformite();
        nc.setId(ncId);
        nc.setCircuit(circuitEnPlace);
        when(nonConformiteRepository.findById(ncId)).thenReturn(Optional.of(nc));
        return nc;
    }

    private void decision(Map<String, String> champs) {
        service.updateWorkflowState(ncId, "EN_COURS", "Validation RQ", "VALIDATION_RQ", champs);
    }

    @Test
    @DisplayName("Le circuit choisi à l'étape est inscrit sur le dossier")
    void circuitChoisi_inscritSurLeDossier() {
        NonConformite nc = dossier(null);

        decision(Map.of("circuitTraitement", "CORRECTION"));

        assertThat(nc.getCircuit()).isEqualTo(Circuit.CORRECTION);
    }

    @Test
    @DisplayName("Le libellé affiché vaut le nom de la constante")
    void libelleAffiche_reconnu() {
        // Le moteur ne transporte que des chaînes, et la valeur arrive telle que la liste de choix
        // l'a produite. Refuser un libellé aurait fait perdre en silence le choix du responsable
        // qualité, alors que sa décision, elle, était déjà enregistrée.
        NonConformite nc = dossier(null);

        decision(Map.of("circuitTraitement", "Action corrective"));

        assertThat(nc.getCircuit()).isEqualTo(Circuit.ACTION_CORRECTIVE);
    }

    @Test
    @DisplayName("Les anciennes lettres sont encore reconnues")
    void anciennesLettres_reconnues() {
        // Les deux valeurs s'appelaient « A » et « B ». La base a été reprise, mais un export ou un
        // client resté en arrière peut encore en porter une.
        NonConformite nc = dossier(null);

        decision(Map.of("circuitTraitement", "B"));

        assertThat(nc.getCircuit()).isEqualTo(Circuit.CORRECTION);
    }

    @Test
    @DisplayName("Une valeur inconnue laisse le dossier sur le circuit qu'il portait")
    void valeurInconnue_circuitInchange() {
        // La décision est déjà jouée côté moteur : la refuser ici la ferait rejouer sans fin.
        NonConformite nc = dossier(Circuit.ACTION_CORRECTIVE);

        decision(Map.of("circuitTraitement", "Quelque chose d'autre"));

        assertThat(nc.getCircuit()).isEqualTo(Circuit.ACTION_CORRECTIVE);
    }

    @Test
    @DisplayName("Une décision qui ne qualifie rien ne change pas le circuit")
    void aucunChoix_circuitInchange() {
        // Toutes les étapes ne posent pas cette question : seule la validation qualité l'ouvre, et
        // la clôture sans suite, jouée depuis la même étape, ne la pose pas non plus.
        NonConformite nc = dossier(Circuit.CORRECTION);

        decision(Map.of("motifClotureDirecte", "Signalement sans objet"));

        assertThat(nc.getCircuit()).isEqualTo(Circuit.CORRECTION);
    }
}
