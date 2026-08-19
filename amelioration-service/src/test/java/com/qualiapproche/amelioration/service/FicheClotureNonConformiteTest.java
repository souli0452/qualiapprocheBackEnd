package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.impl.FicheClotureNonConformiteService;
import com.qualiapproche.amelioration.utils.ReglagesOrganisation;
import com.qualiapproche.common.config.ThymeleafConfig;
import com.qualiapproche.common.dto.ValidationHistoryDto;
import com.qualiapproche.common.enumeration.Circuit;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.utils.StatutEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La fiche de clôture est le document d'enregistrement du dossier : elle ne s'édite qu'en fin de
 * circuit, et jamais sans ses visas — un enregistrement qualité où aucun niveau n'aurait signé
 * n'enregistre rien.
 */
class FicheClotureNonConformiteTest {

    private static final UUID DOSSIER = UUID.fromString("77777777-0000-4000-8000-000000000077");

    private NonConformiteRepository nonConformiteRepository;
    private PlanActionRepository planActionRepository;
    private WorkflowClient workflowClient;
    private ReglagesOrganisation reglages;
    private FicheClotureNonConformiteService service;

    @BeforeEach
    void setUp() {
        nonConformiteRepository = mock(NonConformiteRepository.class);
        planActionRepository = mock(PlanActionRepository.class);
        workflowClient = mock(WorkflowClient.class);
        reglages = mock(ReglagesOrganisation.class);
        // Le moteur de gabarits réel, celui-là même que le service emploie en production : le test
        // vaut aussi preuve que le gabarit se rend sans erreur.
        service = new FicheClotureNonConformiteService(nonConformiteRepository, planActionRepository,
                workflowClient, reglages, ThymeleafConfig.getTemplateEngine(),
                "https://qualisira.exemple.org/non-conformite/suivi?ncId={id}");

        // Les plans sont rattachés par leur colonne nonConformeId, qui fait foi — la collection de
        // la fiche (table de jointure) reste vide pour les plans créés par le chemin normal. La
        // fiche doit donc les lire au dépôt, pas sur l'entité.
        lenient().when(planActionRepository.findPlanActionsByNonConformeId(DOSSIER)).thenReturn(List.of(
                PlanAction.builder()
                        .numeroOdre("1")
                        .actionCorrective("Mettre à jour la procédure d'achat")
                        .causeIdentifiees("Procédure obsolète")
                        .solutionRetenues("Révision de la procédure et diffusion")
                        .responsableNomComplet("Idrissa Ouédraogo")
                        .dateEcheance(LocalDate.of(2026, 5, 15))
                        .status(StatutEnum.TRAITER)
                        .build()));
    }

    private NonConformite dossierCloture() {
        NonConformite nc = NonConformite.builder()
                .id(DOSSIER)
                .numeroReference("NC-2026-014")
                .version("1.0")
                .etatTraitement(Etat.CLOTURE)
                .circuit(Circuit.ACTION_CORRECTIVE)
                .justification("Écart constaté entre la procédure et la pratique.")
                .structureSoumissionLibelle("Direction des achats")
                .nomProcessus("Achats")
                .userImputFullName("Awa Traoré")
                .createdAt(LocalDateTime.of(2026, 3, 2, 9, 30))
                .build();
        return nc;
    }

    private ValidationHistoryDto visa(String etape, String decision, LocalDateTime date) {
        return ValidationHistoryDto.builder()
                .stepName(etape)
                .decision(decision)
                .validatorFullName("Mariam Sankara")
                .comments("Conforme aux attentes")
                .decisionDate(date)
                .build();
    }

    @Test
    @DisplayName("Un dossier clôturé s'édite en PDF, plans et visas compris")
    void dossierCloture_rendUnPdf() {
        when(nonConformiteRepository.findById(DOSSIER)).thenReturn(Optional.of(dossierCloture()));
        when(workflowClient.historiqueDesDecisions(DOSSIER)).thenReturn(List.of(
                visa("Réception", "Réceptionner", LocalDateTime.of(2026, 3, 3, 10, 0)),
                visa("Suivi RQ", "Clôturer", LocalDateTime.of(2026, 6, 1, 16, 45))));

        FicheClotureNonConformiteService.FicheEditee fiche = service.editer(DOSSIER);

        assertThat(fiche.nomDeFichier()).isEqualTo("Fiche_NC_NC-2026-014.pdf");
        assertThat(new String(fiche.contenu(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Tant que le circuit n'est pas terminé, la fiche est refusée")
    void dossierEnCours_estRefuse() {
        NonConformite enCours = dossierCloture();
        enCours.setEtatTraitement(Etat.SUIVI_RQ);
        when(nonConformiteRepository.findById(DOSSIER)).thenReturn(Optional.of(enCours));

        assertThatThrownBy(() -> service.editer(DOSSIER))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    @DisplayName("Sans aucune décision enregistrée, la fiche est refusée : rien à viser")
    void sansVisas_estRefuse() {
        when(nonConformiteRepository.findById(DOSSIER)).thenReturn(Optional.of(dossierCloture()));
        when(workflowClient.historiqueDesDecisions(DOSSIER)).thenReturn(List.of());

        assertThatThrownBy(() -> service.editer(DOSSIER))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    @DisplayName("Moteur injoignable : la fiche attend, elle ne s'imprime pas sans visas")
    void moteurInjoignable_estRefuse() {
        when(nonConformiteRepository.findById(DOSSIER)).thenReturn(Optional.of(dossierCloture()));
        when(workflowClient.historiqueDesDecisions(DOSSIER)).thenThrow(new RuntimeException("connexion refusée"));

        assertThatThrownBy(() -> service.editer(DOSSIER))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
    }
}
