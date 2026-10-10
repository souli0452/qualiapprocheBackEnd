package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.ActivitePlanDto;
import com.qualiapproche.evaluation.dto.ChecklistAuditDto;
import com.qualiapproche.evaluation.dto.PlanAuditDto;
import com.qualiapproche.evaluation.dto.PointControleDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.ChecklistAudit;
import com.qualiapproche.evaluation.entities.PlanAudit;
import com.qualiapproche.evaluation.entities.PointControle;
import com.qualiapproche.evaluation.entities.TypeAudit;
import com.qualiapproche.evaluation.enumeration.OrigineDocument;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutChecklist;
import com.qualiapproche.evaluation.enumeration.StatutPlanAudit;
import com.qualiapproche.evaluation.repository.ChecklistAuditRepository;
import com.qualiapproche.evaluation.repository.PlanAuditRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le plan d'audit et les checklists : ce qui se prépare en brouillon, et ce qui ne se retouche
 * plus une fois annoncé.
 *
 * <p>Un plan partagé avec l'audité, une checklist publiée sont des engagements : les modifier en
 * place changerait après coup ce que l'audité a reçu, ou ce que des constats citent. On duplique,
 * on ne réécrit pas.</p>
 */
class PlanEtChecklistsDAuditTest {

    private static final UUID DIRECTION = UUID.randomUUID();

    @BeforeEach
    void connecter() {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject("responsable-equipe").claim("direction_id", DIRECTION.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class PlanDAudit {

        @Mock private PlanAuditRepository depot;
        @Mock private AuditService audits;
        @Mock private CodificationDocumentService codification;
        @Mock private FichiersAudit fichiers;

        @InjectMocks private PlanAuditService service;

        private Audit audit;

        @BeforeEach
        void preparer() {
            audit = Audit.builder().reference("AUD-2026-030").statut(StatutAudit.EN_PREPARATION)
                    .objectifsAudit("Vérifier la maîtrise des achats").porteeAudit("Site de Ouagadougou").build();
            audit.setId(UUID.randomUUID());
            audit.setDirectionId(DIRECTION);
            when(audits.sienOuRien(audit.getId())).thenReturn(audit);
            when(depot.findByAuditId(audit.getId())).thenReturn(Optional.empty());
            when(depot.save(any(PlanAudit.class))).thenAnswer(i -> i.getArgument(0));
            when(codification.genererReference(CodificationDocumentService.PLAN_AUDIT)).thenReturn("PLA-2026-001");
        }

        private PlanAudit planExistant(StatutPlanAudit statut) {
            PlanAudit plan = PlanAudit.builder().audit(audit).reference("PLA-2026-001")
                    .origine(OrigineDocument.GENERE).statut(statut).build();
            when(depot.findByAuditId(audit.getId())).thenReturn(Optional.of(plan));
            return plan;
        }

        @Test
        @DisplayName("Le premier enregistrement amorce le plan en brouillon, référencé, à partir de l'audit")
        void le_premier_enregistrement_amorce_le_plan() {
            PlanAuditDto plan = service.enregistrer(audit.getId(), PlanAuditDto.builder()
                    .objectifs("Vérifier la maîtrise des achats")
                    .activites(List.of(ActivitePlanDto.builder().horaireDebut("08:30").horaireFin("09:00")
                            .libelleActivite("Réunion d'ouverture").build()))
                    .build());

            assertThat(plan.getReference()).isEqualTo("PLA-2026-001");
            assertThat(plan.getStatut()).isEqualTo(StatutPlanAudit.BROUILLON);
            assertThat(plan.getOrigine()).isEqualTo(OrigineDocument.GENERE);
            assertThat(plan.getActivites()).extracting(ActivitePlanDto::getLibelleActivite)
                    .containsExactly("Réunion d'ouverture");
            assertThat(plan.getMethodesAudit()).isEmpty();
        }

        @Test
        @DisplayName("Le plan d'un audit clôturé ou annulé ne se modifie plus")
        void le_plan_d_un_audit_termine_ne_se_modifie_plus() {
            audit.setStatut(StatutAudit.ANNULE);

            assertThatThrownBy(() -> service.enregistrer(audit.getId(), PlanAuditDto.builder().build()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("ne se modifie plus");
            verify(depot, never()).save(any());
        }

        @Test
        @DisplayName("Le plan d'un audit externe ne s'élabore pas ici : il se charge")
        void le_plan_d_un_audit_externe_se_charge() {
            audit.setType(TypeAudit.builder().libelle("Certification").circuitExterne(true).build());

            assertThatThrownBy(() -> service.enregistrer(audit.getId(), PlanAuditDto.builder().build()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("organisme auditeur");
        }

        @Test
        @DisplayName("Un plan validé ne se réécrit plus, ni par saisie ni par chargement")
        void un_plan_valide_ne_se_reecrit_plus() {
            planExistant(StatutPlanAudit.VALIDE);

            assertThatThrownBy(() -> service.enregistrer(audit.getId(), PlanAuditDto.builder().lieu("Bobo").build()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("ne se modifie plus");
            assertThatThrownBy(() -> service.charger(audit.getId(), mock(MultipartFile.class)))
                    .isInstanceOf(BusinessException.class);
            verify(fichiers, never()).deposer(any(), any(String[].class));
        }

        @Test
        @DisplayName("Un plan vide ne se valide pas : il n'annoncerait rien à l'audité")
        void un_plan_vide_ne_se_valide_pas() {
            planExistant(StatutPlanAudit.BROUILLON);

            assertThatThrownBy(() -> service.valider(audit.getId()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("sans activité");
        }

        @Test
        @DisplayName("Un plan chargé se valide, puis se partage avec l'audité à une date consignée")
        void un_plan_charge_se_valide_puis_se_partage() {
            audit.setType(TypeAudit.builder().libelle("Certification").circuitExterne(true).build());
            planExistant(StatutPlanAudit.BROUILLON);
            when(fichiers.deposer(any(), any(String[].class))).thenReturn("audits/plan.pdf");

            assertThat(service.charger(audit.getId(), mock(MultipartFile.class)).getOrigine())
                    .isEqualTo(OrigineDocument.CHARGE);
            assertThat(service.valider(audit.getId()).getStatut()).isEqualTo(StatutPlanAudit.VALIDE);
            PlanAuditDto partage = service.partagerAvecAudite(audit.getId());

            assertThat(partage.getStatut()).isEqualTo(StatutPlanAudit.PARTAGE);
            assertThat(partage.getDatePartage()).isNotNull();
            assertThat(partage.isFichierCharge()).isTrue();
        }

        @Test
        @DisplayName("Le plan reçu d'un organisme garde son nom de fichier et ne prend pas de numéro PLA")
        void le_plan_recu_garde_son_nom_sans_numero() {
            audit.setType(TypeAudit.builder().libelle("Certification").circuitExterne(true).build());
            when(fichiers.deposer(any(), any(String[].class))).thenReturn("audits/plan.pdf");
            MultipartFile fichier = mock(MultipartFile.class);
            when(fichier.getOriginalFilename()).thenReturn("C:\\Users\\awa\\Plan AFNOR 2026.pdf");

            PlanAuditDto plan = service.charger(audit.getId(), fichier);

            assertThat(plan.getNomFichierCharge()).isEqualTo("Plan AFNOR 2026.pdf");
            assertThat(plan.getReference()).isNull();
            verify(codification, never()).genererReference(any());
        }

        @Test
        @DisplayName("Le plan chargé se rend sous son nom d'origine ; un ancien chargement, sous un nom générique")
        void le_plan_charge_se_rend_sous_son_nom() {
            PlanAudit plan = planExistant(StatutPlanAudit.BROUILLON);
            assertThat(service.nomDuFichier(audit.getId())).isEqualTo("plan-audit");

            plan.setNomFichierRecu("Plan AFNOR 2026.pdf");
            assertThat(service.nomDuFichier(audit.getId())).isEqualTo("Plan AFNOR 2026.pdf");
        }

        @Test
        @DisplayName("Un plan ne se charge ni sur un audit interne, ni sur un audit clôturé")
        void pas_de_plan_charge_hors_audit_externe_vivant() {
            assertThatThrownBy(() -> service.charger(audit.getId(), mock(MultipartFile.class)))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("audit interne");

            audit.setType(TypeAudit.builder().libelle("Certification").circuitExterne(true).build());
            audit.setStatut(StatutAudit.CLOTURE);
            assertThatThrownBy(() -> service.charger(audit.getId(), mock(MultipartFile.class)))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("ne se remplace plus");
            verify(fichiers, never()).deposer(any(), any(String[].class));
        }

        @Test
        @DisplayName("Un plan encore en brouillon ne se partage pas")
        void un_brouillon_ne_se_partage_pas() {
            planExistant(StatutPlanAudit.BROUILLON);

            assertThatThrownBy(() -> service.partagerAvecAudite(audit.getId()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("Seul un plan validé");
        }

        @Test
        @DisplayName("Lire le plan d'un audit qui n'en a pas répond 404")
        void pas_de_plan_404() {
            assertThatThrownBy(() -> service.lire(audit.getId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Checklists {

        @Mock private ChecklistAuditRepository depot;
        @Mock private AuditeurService auditeurs;

        @InjectMocks private ChecklistAuditService service;

        private ChecklistAudit checklist;
        private PointControle point;

        @BeforeEach
        void preparer() {
            checklist = ChecklistAudit.builder().nom("Achats").statut(StatutChecklist.BROUILLON).build();
            checklist.setId(UUID.randomUUID());
            checklist.setDirectionId(DIRECTION);
            point = PointControle.builder().checklist(checklist).libelle("Bons de commande").build();
            point.setId(UUID.randomUUID());
            checklist.getPoints().add(point);
            when(depot.findById(checklist.getId())).thenReturn(Optional.of(checklist));
            when(depot.save(any(ChecklistAudit.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("Un point repris garde son identité, un nouveau est créé, un absent est retiré")
        void les_points_se_recopient_par_identifiant() {
            ChecklistAuditDto modifiee = service.modifier(checklist.getId(), ChecklistAuditDto.builder()
                    .nom("  Achats et approvisionnements ")
                    .points(List.of(
                            PointControleDto.builder().id(point.getId()).libelle("Bons de commande signés").build(),
                            PointControleDto.builder().libelle("Réception des livraisons").build()))
                    .build());

            assertThat(modifiee.getNom()).isEqualTo("Achats et approvisionnements");
            assertThat(checklist.getPoints()).hasSize(2);
            assertThat(checklist.getPoints().get(0)).isSameAs(point);
            assertThat(point.getLibelle()).isEqualTo("Bons de commande signés");
            assertThat(checklist.getPoints().get(1).getChecklist()).isSameAs(checklist);
        }

        @Test
        @DisplayName("Une checklist publiée ne se modifie ni ne se supprime : elle se duplique")
        void une_checklist_publiee_se_duplique() {
            checklist.setStatut(StatutChecklist.PUBLIEE);

            assertThatThrownBy(() -> service.modifier(checklist.getId(),
                    ChecklistAuditDto.builder().nom("Achats").build()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("dupliquez-la");
            assertThatThrownBy(() -> service.supprimer(checklist.getId())).isInstanceOf(BusinessException.class);
            verify(depot, never()).delete(any(ChecklistAudit.class));

            ChecklistAuditDto copie = service.dupliquer(checklist.getId());
            assertThat(copie.getStatut()).isEqualTo(StatutChecklist.BROUILLON);
            assertThat(copie.getNom()).isEqualTo("Achats (copie)");
            assertThat(copie.getPoints()).hasSize(1);
        }

        @Test
        @DisplayName("Une checklist sans point de contrôle ne se publie pas")
        void une_checklist_vide_ne_se_publie_pas() {
            checklist.getPoints().clear();

            assertThatThrownBy(() -> service.publier(checklist.getId()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("aucun point");
            assertThat(checklist.getStatut()).isEqualTo(StatutChecklist.BROUILLON);
        }

        @Test
        @DisplayName("La checklist d'une autre direction est introuvable")
        void la_checklist_d_une_autre_direction_est_introuvable() {
            checklist.setDirectionId(UUID.randomUUID());

            assertThatThrownBy(() -> service.lire(checklist.getId()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("Aucun(e) checklist");
        }
    }
}
