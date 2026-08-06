package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteResumeMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlanActionServiceImpl;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.dto.PlanActionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une action corrective ne se comprend pas sans le dossier qui l'a motivée.
 *
 * <p>L'action ne portait que l'identifiant de sa non-conformité — une valeur qu'aucun écran ne peut
 * afficher — et la colonne prévue pour le numéro du dossier n'était écrite par personne. Le
 * responsable qui devait traiter l'action ne lisait donc nulle part ce qui avait été constaté, sur
 * quel processus, ni sous quel numéro : il lui fallait rouvrir la non-conformité dans une autre
 * liste pour savoir ce qu'on lui demandait de corriger.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanActionPorteSonDossierTest {

    @Mock private PlanActionRepository planActionRepository;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private NonConformiteResumeMapper nonConformiteResumeMapper;
    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PieceJointeStockageService fichierService;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;

    @InjectMocks private PlanActionServiceImpl service;

    private final UUID dossierId = UUID.randomUUID();
    private NonConformite dossier;

    @BeforeEach
    void dossierExistant() {
        dossier = new NonConformite();
        dossier.setId(dossierId);
        dossier.setNumeroReference("NC-2026-014");
        dossier.setNomProcessus("Gestion des achats");
        when(nonConformiteRepository.findById(dossierId)).thenReturn(Optional.of(dossier));
        when(nonConformiteRepository.findAllById(any())).thenReturn(List.of(dossier));
        when(nonConformiteResumeMapper.versResume(dossier)).thenReturn(new NonConformiteDto());
        when(planActionRepository.save(any(PlanAction.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("La création recopie sur l'action les repères de son dossier")
    void creation_inscritLesReperesDuDossier() throws IOException {
        PlanActionDto demande = new PlanActionDto();
        demande.setNonConformeId(dossierId);
        when(planActionMapper.toEntity(demande)).thenReturn(new PlanAction());
        when(planActionMapper.toDto(any(PlanAction.class))).thenReturn(new PlanActionDto());

        service.createPlanActionDto(demande);

        // Ni l'écran ni le serveur ne l'écrivaient : la recherche par numéro ne trouvait aucune
        // action, et les relances d'échéance en annonçaient une rattachée à « null ».
        ArgumentCaptor<PlanAction> enregistree = ArgumentCaptor.forClass(PlanAction.class);
        verify(planActionRepository).save(enregistree.capture());
        assertThat(enregistree.getValue().getNumeroNc()).isEqualTo("NC-2026-014");
        assertThat(enregistree.getValue().getProcEmetteur()).isEqualTo("Gestion des achats");
    }

    @Test
    @DisplayName("La fiche d'une action porte le dossier qui l'a motivée")
    void lecture_jointLeDossier() {
        PlanAction plan = new PlanAction();
        plan.setId(UUID.randomUUID());
        plan.setNonConformeId(dossierId);
        when(planActionRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

        PlanActionDto ancienne = new PlanActionDto();
        ancienne.setNonConformeId(dossierId);
        when(planActionMapper.toDto(plan)).thenReturn(ancienne);

        PlanActionDto dto = service.getPlanActionDtoById(plan.getId());

        assertThat(dto.getNonConformite()).isNotNull();
        // Les actions créées avant que la création ne recopie le numéro ont la colonne vide : il est
        // relu du dossier plutôt que de les afficher amputées en attendant une reprise de données.
        assertThat(dto.getNumeroNc()).isEqualTo("NC-2026-014");
        assertThat(dto.getProcEmetteur()).isEqualTo("Gestion des achats");
    }

    @Test
    @DisplayName("Les repères déjà portés par l'action font foi")
    void lecture_neRecrasePasCeQuiEstPorte() {
        PlanAction plan = new PlanAction();
        plan.setId(UUID.randomUUID());
        plan.setNonConformeId(dossierId);
        when(planActionRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

        PlanActionDto porte = new PlanActionDto();
        porte.setNonConformeId(dossierId);
        porte.setNumeroNc("NC-2025-001");
        porte.setProcEmetteur("Logistique");
        when(planActionMapper.toDto(plan)).thenReturn(porte);

        PlanActionDto dto = service.getPlanActionDtoById(plan.getId());

        assertThat(dto.getNumeroNc()).isEqualTo("NC-2025-001");
        assertThat(dto.getProcEmetteur()).isEqualTo("Logistique");
    }
}
