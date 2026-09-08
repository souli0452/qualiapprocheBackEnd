package com.qualiapproche.amelioration.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.NonConformiteServiceImpl;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.service.SendMailService;

/**
 * L'agent imputé sur la fiche et le titulaire du circuit disent la même chose.
 *
 * <p>Ce sont deux inscriptions d'une seule décision : l'étape d'imputation renseigne son
 * {@code champTitulaire}, le moteur en fait son {@code titulaireId} et le module son
 * {@code userImputId}. Rien ne les rattachait ensuite. Une réaffectation faite depuis la fiche
 * changeait l'imputé sans que le moteur en sache rien, et les deux se mettaient à répondre
 * différemment à la même question : <b>l'étape réservée au titulaire restait ouverte à celui qui ne
 * répondait plus du dossier</b>, cependant que les listes du module — qui lisent l'imputé — le
 * montraient au nouveau. Un dossier compté chez l'un, affiché chez l'autre.</p>
 *
 * <p>Le plan d'action tenait déjà les deux ensemble ({@code PlanActionServiceImpl}) ; la
 * non-conformité, non. Le créateur, lui, n'a rien à rattraper : il est inscrit à l'ouverture des
 * deux côtés depuis la même requête, et jamais réécrit.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImputationEtTitulaireDAccordTest {

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

    private static final String ANCIEN = "agent-1";
    private static final String NOUVEAU = "agent-2";

    private final UUID ncId = UUID.randomUUID();

    /** Un dossier déjà engagé dans un circuit, imputé à quelqu'un. */
    private NonConformite dossierImpute(String agent) {
        NonConformite nc = new NonConformite();
        nc.setId(ncId);
        nc.setUserImputId(agent);
        nc.setWorkflowId(UUID.randomUUID());
        when(nonConformiteRepository.findById(ncId)).thenReturn(Optional.of(nc));
        when(nonConformiteRepository.save(any(NonConformite.class))).thenAnswer(a -> a.getArgument(0));
        // Le mapper est simulé et n'inscrit rien — l'imputation ne passe plus par lui, c'est le
        // service qui l'applique. Un retour non nul tout de même, sans quoi « Optional.map »
        // rendrait vide et le point d'entrée croirait le dossier introuvable.
        when(nonConformiteMapper.toDto(any(NonConformite.class))).thenReturn(new NonConformiteDto());
        return nc;
    }

    /** Réaffectation décidée depuis la fiche, hors circuit. */
    private void reaffecter(String agent) {
        NonConformiteDto saisie = new NonConformiteDto();
        saisie.setId(ncId);
        saisie.setUserImputId(agent);
        service.update(saisie);
    }

    @Test
    @DisplayName("Une réaffectation depuis la fiche redésigne le titulaire du circuit")
    void reaffectation_redesigneLeTitulaire() {
        dossierImpute(ANCIEN);

        reaffecter(NOUVEAU);

        // Sans cet appel, l'étape réservée au titulaire serait restée ouverte à l'ancien.
        verify(workflowClient).designerTitulaire(ncId, NOUVEAU);
    }

    @Test
    @DisplayName("Une fiche enregistrée sans changer l'imputé ne dérange pas le moteur")
    void memeAgent_aucunAppel() {
        dossierImpute(ANCIEN);

        reaffecter(ANCIEN);

        verify(workflowClient, never()).designerTitulaire(any(), anyString());
    }

    @Test
    @DisplayName("Une saisie sans imputation ne désimpute pas la fiche et ne redésigne personne")
    void imputationAbsente_laisseeEnPlace() {
        // Tous les écrans qui enregistrent une fiche ne montrent pas l'imputation. Prendre leur
        // silence pour un retrait désimputait le dossier — le mapper recopiait le null — et aurait
        // fermé l'étape à tous si le moteur l'avait appris. Absent n'est pas vide.
        NonConformite nc = dossierImpute(ANCIEN);

        reaffecter(null);

        assertThat(nc.getUserImputId()).isEqualTo(ANCIEN);
        verify(workflowClient, never()).designerTitulaire(any(), anyString());
    }

    @Test
    @DisplayName("La fiche complète enregistrée sans imputation garde son agent elle aussi")
    void ficheCompleteSansImputation_agentConserve() throws Exception {
        // La voie principale de mise à jour écrivait l'imputation sans condition, y compris à null,
        // là où ses champs voisins sont gardés par un « si renseigné ». Même règle sur les trois
        // voies : c'est une seule décision, elle ne peut pas dépendre de l'écran qui enregistre.
        NonConformite nc = dossierImpute(ANCIEN);
        NonConformiteDto saisie = new NonConformiteDto();
        saisie.setId(ncId);

        service.updateNonConformite(ncId, saisie);

        assertThat(nc.getUserImputId()).isEqualTo(ANCIEN);
        verify(workflowClient, never()).designerTitulaire(any(), anyString());
    }

    @Test
    @DisplayName("Un dossier sans circuit ouvert n'a aucun titulaire à redésigner")
    void sansCircuit_aucunAppel() {
        NonConformite nc = dossierImpute(ANCIEN);
        nc.setWorkflowId(null);

        reaffecter(NOUVEAU);

        verify(workflowClient, never()).designerTitulaire(any(), anyString());
    }

    @Test
    @DisplayName("Moteur injoignable : la réaffectation est refusée plutôt que faite à moitié")
    void moteurInjoignable_reaffectationRefusee() {
        // Une fiche qui annonce un nouvel imputé pendant que le circuit ouvre encore l'étape à
        // l'ancien est pire que pas de transfert du tout : les deux se contredisent sans que rien
        // ne le signale. Même refus que pour le responsable d'un plan d'action.
        NonConformite nc = dossierImpute(ANCIEN);
        doThrow(new IllegalStateException("workflow-service injoignable"))
                .when(workflowClient).designerTitulaire(any(), anyString());

        assertThatThrownBy(() -> reaffecter(NOUVEAU))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("annulé plutôt que laissé à moitié fait");
    }

    @Test
    @DisplayName("L'imputation décidée par le circuit ne revient pas au moteur")
    void decisionDuCircuit_pasDeRetour() {
        // L'autre sens fonctionne déjà : le moteur inscrit son titulaire, puis annonce la décision
        // au module qui range l'agent sur la fiche. Le lui renvoyer serait un aller-retour inutile,
        // et sur un moteur momentanément muet, un refus de la décision qu'il vient de prendre.
        NonConformite nc = new NonConformite();
        nc.setId(ncId);
        nc.setWorkflowId(UUID.randomUUID());
        when(nonConformiteRepository.findById(ncId)).thenReturn(Optional.of(nc));

        service.updateWorkflowState(ncId, "EN_COURS", "Traitement", "TRAITEMENT",
                Map.of("userImputId", NOUVEAU));

        assertThat(nc.getUserImputId()).isEqualTo(NOUVEAU);
        verify(workflowClient, never()).designerTitulaire(any(), anyString());
    }
}
