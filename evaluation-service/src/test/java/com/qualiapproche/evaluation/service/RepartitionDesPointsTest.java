package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.RepartitionPointDto;
import com.qualiapproche.evaluation.entities.AffectationPoint;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.ChecklistAudit;
import com.qualiapproche.evaluation.entities.PointControle;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.repository.AffectationPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Qui renseigne quel point sur un audit : la répartition de l'audit, puis le modèle si son auditeur
 * est de l'équipe, puis toute l'équipe (recette du 17 oct. 2026, observation 20).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepartitionDesPointsTest {

    @Mock private AffectationPointRepository affectations;
    @Mock private AuditService audits;
    @Mock private AuditeurService auditeurs;
    @Mock private PermissionChecker permissions;

    @InjectMocks private RepartitionDesPoints repartition;

    private Auditeur responsable;
    private Auditeur membre;
    private Auditeur horsEquipe;
    private PointControle pointDuModele;
    private PointControle pointLibre;
    private Audit audit;

    private static Auditeur auditeur() {
        Auditeur a = Auditeur.builder().utilisateurId(UUID.randomUUID().toString()).build();
        a.setId(UUID.randomUUID());
        return a;
    }

    @BeforeEach
    void preparer() {
        responsable = auditeur();
        membre = auditeur();
        horsEquipe = auditeur();
        ChecklistAudit checklist = ChecklistAudit.builder().nom("ISO 9001 — §7").points(new ArrayList<>()).build();
        pointDuModele = PointControle.builder().checklist(checklist).libelle("Maîtrise documentaire").auditeurAssigne(horsEquipe).build();
        pointDuModele.setId(UUID.randomUUID());
        pointLibre = PointControle.builder().checklist(checklist).libelle("Compétences").build();
        pointLibre.setId(UUID.randomUUID());
        checklist.getPoints().addAll(List.of(pointDuModele, pointLibre));
        audit = Audit.builder().reference("QS-2026-007").statut(StatutAudit.EN_COURS).responsableEquipe(responsable)
                .membresEquipe(new java.util.LinkedHashSet<>(List.of(membre)))
                .checklists(new java.util.LinkedHashSet<>(List.of(checklist))).build();
        audit.setId(UUID.randomUUID());
        when(audits.sienOuRien(audit.getId())).thenReturn(audit);
        when(affectations.findAllByAuditId(audit.getId())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Un point du modèle confié à quelqu'un hors de l'équipe s'offre à toute l'équipe")
    void point_hors_equipe_offert_a_toute_l_equipe() {
        assertThat(repartition.attribueA(audit, pointDuModele, Map.of())).isNull();
    }

    @Test
    @DisplayName("La répartition de l'audit prime sur le modèle")
    void la_repartition_de_l_audit_prime() {
        assertThat(repartition.attribueA(audit, pointDuModele, Map.of(pointDuModele.getId(), membre.getId())))
                .isEqualTo(membre.getId());
    }

    @Test
    @DisplayName("Le responsable répartit les points entre les membres de l'équipe")
    void le_responsable_repartit() {
        when(auditeurs.auditeurCourant()).thenReturn(responsable);

        repartition.repartir(audit.getId(), List.of(
                RepartitionPointDto.builder().pointId(pointDuModele.getId()).auditeurId(membre.getId()).build(),
                RepartitionPointDto.builder().pointId(pointLibre.getId()).build()));

        verify(affectations).deleteAllByAuditId(audit.getId());
        verify(affectations).save(org.mockito.ArgumentMatchers.argThat((AffectationPoint a) ->
                a.getPointControle() == pointDuModele && a.getAuditeur() == membre));
    }

    @Test
    @DisplayName("Un point ne se confie pas à un auditeur hors de l'équipe")
    void pas_d_auditeur_hors_equipe() {
        when(auditeurs.auditeurCourant()).thenReturn(responsable);

        assertThatThrownBy(() -> repartition.repartir(audit.getId(), List.of(
                RepartitionPointDto.builder().pointId(pointLibre.getId()).auditeurId(horsEquipe.getId()).build())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("pas de l'équipe");
    }

    @Test
    @DisplayName("Un simple membre ne répartit pas les points")
    void un_membre_ne_repartit_pas() {
        when(auditeurs.auditeurCourant()).thenReturn(membre);

        assertThatThrownBy(() -> repartition.repartir(audit.getId(), List.of()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("responsable d'équipe");
        verify(affectations, never()).save(any());
    }
}
