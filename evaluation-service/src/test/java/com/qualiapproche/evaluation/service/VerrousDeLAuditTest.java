package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.client.Parametrage;
import com.qualiapproche.evaluation.dto.AuditDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutAuditeur;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.SignatureRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ce qu'un audit validé, puis un rapport visé, ne laissent plus faire : retoucher l'équipe sans
 * ses contrôles, changer après coup ce qu'un signataire a lu.
 */
class VerrousDeLAuditTest {

    private static final UUID DIRECTION = UUID.randomUUID();
    private static final LocalDate DEBUT = LocalDate.of(2026, 11, 2);
    private static final LocalDate FIN = LocalDate.of(2026, 11, 4);

    static void connecter() {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject("auditeur-1").claim("direction_id", DIRECTION.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    static Audit audit(StatutAudit statut) {
        Audit audit = Audit.builder().reference("AUD-2026-007").statut(statut)
                .processusId(UUID.randomUUID()).dateDebutPrevue(DEBUT).dateFinPrevue(FIN).build();
        audit.setId(UUID.randomUUID());
        audit.setDirectionId(DIRECTION);
        return audit;
    }

    static Auditeur auditeur() {
        Auditeur auditeur = Auditeur.builder().utilisateurId("auditeur-" + UUID.randomUUID())
                .statut(StatutAuditeur.ACTIF).build();
        auditeur.setId(UUID.randomUUID());
        auditeur.setDirectionId(DIRECTION);
        return auditeur;
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    /** Retoucher un audit déjà validé repasse par les contrôles de la validation. */
    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class RetoucheDUnAuditValide {

        @Mock private AuditRepository depot;
        @Mock private AuditeurService auditeurs;
        @Mock private SignatureRepository signatures;
        @Mock private Parametrage parametrage;

        @InjectMocks private AuditService service;

        private final Auditeur responsable = auditeur();
        private final Auditeur membre = auditeur();
        private Audit audit;

        @BeforeEach
        void preparer() {
            connecter();
            audit = audit(StatutAudit.EN_PREPARATION);
            audit.setResponsableEquipe(responsable);
            when(depot.findById(audit.getId())).thenReturn(Optional.of(audit));
            when(depot.save(any(Audit.class))).thenAnswer(i -> i.getArgument(0));
            when(auditeurs.sienOuRien(responsable.getId())).thenReturn(responsable);
            when(auditeurs.sienOuRien(membre.getId())).thenReturn(membre);
            when(auditeurs.estDisponible(any(Auditeur.class), any(), any(), any())).thenReturn(true);
        }

        private AuditDto saisie() {
            return AuditDto.builder().processusId(audit.getProcessusId())
                    .dateDebutPrevue(DEBUT).dateFinPrevue(FIN)
                    .responsableEquipeId(responsable.getId()).membreEquipeIds(List.of(membre.getId())).build();
        }

        @Test
        @DisplayName("Un membre du processus audité ne s'ajoute pas à l'équipe d'un audit validé")
        void un_membre_du_processus_audite_ne_s_ajoute_pas_a_un_audit_valide() {
            membre.setStructureAppartenance(audit.getProcessusId());

            assertThatThrownBy(() -> service.modifier(audit.getId(), saisie()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("ne peut pas l'auditer");
            verify(depot, never()).save(any());
        }

        @Test
        @DisplayName("Un auditeur déjà pris sur la période ne s'ajoute pas à un audit validé")
        void un_auditeur_deja_pris_ne_s_ajoute_pas_a_un_audit_valide() {
            when(auditeurs.estDisponible(eq(membre), any(), any(), any())).thenReturn(false);

            assertThatThrownBy(() -> service.modifier(audit.getId(), saisie()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("déjà engagé");
            verify(depot, never()).save(any());
        }

        @Test
        @DisplayName("Un audit validé ne perd pas son responsable d'équipe")
        void un_audit_valide_ne_perd_pas_son_responsable() {
            AuditDto saisie = saisie();
            saisie.setResponsableEquipeId(null);

            assertThatThrownBy(() -> service.modifier(audit.getId(), saisie))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("responsable");
            verify(depot, never()).save(any());
        }

        @Test
        @DisplayName("Un audit encore planifié se retouche sans contrôle : le programme s'écrit par brouillons")
        void un_audit_planifie_se_retouche_sans_controle() {
            audit.setStatut(StatutAudit.PLANIFIE);
            membre.setStructureAppartenance(audit.getProcessusId());
            AuditDto saisie = saisie();
            saisie.setResponsableEquipeId(null);

            assertThat(service.modifier(audit.getId(), saisie).getMembreEquipeIds()).containsExactly(membre.getId());
        }

        @Test
        @DisplayName("Seuls les sites ajoutés se vérifient : un site retiré depuis reste sur l'audit")
        void seuls_les_sites_ajoutes_se_verifient() {
            UUID dejaLa = UUID.randomUUID();
            UUID ajoute = UUID.randomUUID();
            audit.getSiteIds().add(dejaLa);
            AuditDto saisie = saisie();
            saisie.setSiteIds(List.of(dejaLa, ajoute));

            assertThat(service.modifier(audit.getId(), saisie).getSiteIds()).containsExactly(dejaLa, ajoute);
            verify(parametrage).exigerDesSitesActifs(List.of(ajoute));
        }

        @Test
        @DisplayName("Un site inconnu du paramétrage ne s'ajoute pas")
        void un_site_inconnu_ne_s_ajoute_pas() {
            UUID inconnu = UUID.randomUUID();
            doThrow(new BusinessException("Aucun(e) site actif(ve)", org.springframework.http.HttpStatus.NOT_FOUND))
                    .when(parametrage).exigerDesSitesActifs(List.of(inconnu));
            AuditDto saisie = saisie();
            saisie.setSiteIds(List.of(inconnu));

            assertThatThrownBy(() -> service.modifier(audit.getId(), saisie)).isInstanceOf(BusinessException.class);
            verify(depot, never()).save(any());
        }

        @Test
        @DisplayName("Une retouche conforme d'un audit validé passe")
        void une_retouche_conforme_passe() {
            assertThat(service.modifier(audit.getId(), saisie()).getMembreEquipeIds()).containsExactly(membre.getId());
            verify(depot).save(audit);
        }

        @Test
        @DisplayName("Le rapport visé refuse le geste qui le changerait, et le dit")
        void le_rapport_vise_refuse_le_geste() {
            when(signatures.existsByAuditIdAndStatut(audit.getId(), StatutSignature.SIGNEE)).thenReturn(true);

            assertThatThrownBy(() -> service.exigerRapportSansVisa(audit, "publier un constat"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("publier un constat")
                    .hasMessageContaining("Reposez le circuit");
        }

        @Test
        @DisplayName("Un rapport sans visa laisse faire")
        void un_rapport_sans_visa_laisse_faire() {
            when(signatures.existsByAuditIdAndStatut(audit.getId(), StatutSignature.SIGNEE)).thenReturn(false);

            service.exigerRapportSansVisa(audit, "publier un constat");
        }
    }

    /** Dès le premier visa, aucun constat ne se saisit ni ne se publie. */
    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class ConstatsApresVisa {

        @Mock private ConstatRepository depot;
        @Mock private AuditService audits;
        @Mock private AuditeurService auditeurs;
        @Mock private PermissionChecker permissions;
        @Mock private ApplicationEventPublisher evenements;

        @InjectMocks private ConstatService service;

        private final Auditeur moi = auditeur();
        private Audit audit;
        private Constat compile;

        @BeforeEach
        void preparer() {
            connecter();
            audit = audit(StatutAudit.EN_COURS);
            audit.setResponsableEquipe(moi);
            compile = Constat.builder().audit(audit).redacteur(moi).statut(StatutConstat.COMPILE)
                    .observation("Procédure non à jour").build();
            compile.setId(UUID.randomUUID());
            compile.setDirectionId(DIRECTION);
            when(depot.findById(compile.getId())).thenReturn(Optional.of(compile));
            when(depot.findAllByAuditIdAndStatutOrderByDateSaisieAsc(audit.getId(), StatutConstat.COMPILE))
                    .thenReturn(List.of(compile));
            when(audits.sienOuRien(audit.getId())).thenReturn(audit);
            when(auditeurs.auditeurCourant()).thenReturn(moi);
            doThrow(new BusinessException("visé", org.springframework.http.HttpStatus.CONFLICT))
                    .when(audits).exigerRapportSansVisa(eq(audit), any());
        }

        @Test
        @DisplayName("Un constat compilé ne se publie plus")
        void un_constat_compile_ne_se_publie_plus() {
            assertThatThrownBy(() -> service.valider(compile.getId())).isInstanceOf(BusinessException.class);
            assertThat(compile.getStatut()).isEqualTo(StatutConstat.COMPILE);
            verify(evenements, never()).publishEvent(any());
        }

        @Test
        @DisplayName("La compilation ne se valide plus d'un geste")
        void la_compilation_ne_se_valide_plus() {
            assertThatThrownBy(() -> service.validerLaCompilation(audit.getId(), null))
                    .isInstanceOf(BusinessException.class);
            assertThat(compile.getStatut()).isEqualTo(StatutConstat.COMPILE);
            verify(depot, never()).saveAll(any());
        }

        @Test
        @DisplayName("Aucun nouveau constat ne se saisit")
        void aucun_nouveau_constat_ne_se_saisit() {
            assertThatThrownBy(() -> service.creer(audit.getId(),
                    com.qualiapproche.evaluation.dto.ConstatDto.builder().observation("Écart").build()))
                    .isInstanceOf(BusinessException.class);
            verify(depot, never()).save(any());
        }
    }
}
