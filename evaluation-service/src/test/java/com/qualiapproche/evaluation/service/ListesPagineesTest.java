package com.qualiapproche.evaluation.service;

import com.qualiapproche.evaluation.dto.AuditeurDto;
import com.qualiapproche.evaluation.dto.ChecklistAuditDto;
import com.qualiapproche.evaluation.dto.FiltreVivier;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.ChecklistAudit;
import com.qualiapproche.evaluation.enumeration.StatutChecklist;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.AuditeurRepository;
import com.qualiapproche.evaluation.repository.ChecklistAuditRepository;
import com.qualiapproche.evaluation.repository.EvaluationAuditeurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les listes d'écran du module se servent page par page ; celles des choix restent entières.
 *
 * <p>Deux lectures, deux dépôts : la page passe par les critères, la liste des choix par la
 * requête dérivée — afin qu'un sélecteur ne retombe jamais sur dix éléments.</p>
 */
class ListesPagineesTest {

    private static final UUID MA_DIRECTION = UUID.randomUUID();

    private final ChecklistAuditRepository checklists = mock(ChecklistAuditRepository.class);
    private final AuditeurRepository auditeurs = mock(AuditeurRepository.class);
    private final AuditRepository audits = mock(AuditRepository.class);
    private final EvaluationAuditeurRepository evaluations = mock(EvaluationAuditeurRepository.class);
    private final AuditeurService serviceAuditeurs = new AuditeurService(auditeurs, audits, evaluations);
    private final ChecklistAuditService serviceChecklists = new ChecklistAuditService(checklists, serviceAuditeurs);

    @BeforeEach
    void connecter() {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject("rq").claim("direction_id", MA_DIRECTION.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("La page des checklists se lit par critères, la plus ancienne en tête faute d'ordre demandé")
    void la_page_des_checklists_se_lit_par_criteres() {
        ChecklistAudit publiee = ChecklistAudit.builder().nom("ISO 9001 — chapitre 7").statut(StatutChecklist.PUBLIEE).build();
        publiee.setId(UUID.randomUUID());
        when(checklists.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publiee), PageRequest.of(1, 10), 11));

        Page<ChecklistAuditDto> page = serviceChecklists.page(StatutChecklist.PUBLIEE, "iso", PageRequest.of(1, 10));

        assertThat(page.getTotalElements()).isEqualTo(11);
        assertThat(page.getContent()).extracting(ChecklistAuditDto::getNom).containsExactly("ISO 9001 — chapitre 7");
        ArgumentCaptor<Pageable> demande = ArgumentCaptor.forClass(Pageable.class);
        verify(checklists).findAll(any(Specification.class), demande.capture());
        assertThat(demande.getValue().getPageNumber()).isEqualTo(1);
        assertThat(demande.getValue().getSort()).isEqualTo(Sort.by("createdAt"));
        verify(checklists, never()).findAllByDirectionIdOrderByCreatedAtAsc(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("La liste des choix reste entière : elle ne passe pas par la page")
    void la_liste_des_choix_reste_entiere() {
        Auditeur inscrit = Auditeur.builder().utilisateurId("u-1").build();
        inscrit.setId(UUID.randomUUID());
        when(auditeurs.findAllByDirectionIdOrderByCreatedAtAsc(MA_DIRECTION)).thenReturn(List.of(inscrit));

        List<AuditeurDto> vivier = serviceAuditeurs.lister();

        assertThat(vivier).extracting(AuditeurDto::getUtilisateurId).containsExactly("u-1");
        verify(auditeurs, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("La page du vivier garde l'ordre que l'écran demande")
    void la_page_du_vivier_garde_l_ordre_demande() {
        when(auditeurs.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        Pageable parStatut = PageRequest.of(0, 25, Sort.by("statut"));

        serviceAuditeurs.page(FiltreVivier.builder().utilisateurIds(List.of("u-1", "u-2")).build(), parStatut);

        verify(auditeurs).findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(parStatut));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("La page du vivier porte la moyenne et les audits de l'année de chacun, en requêtes groupées")
    void la_page_du_vivier_porte_les_chiffres_de_chacun() {
        Auditeur awa = Auditeur.builder().utilisateurId("awa").build();
        awa.setId(UUID.randomUUID());
        Auditeur ali = Auditeur.builder().utilisateurId("ali").build();
        ali.setId(UUID.randomUUID());
        when(auditeurs.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(awa, ali)));
        when(evaluations.moyennes(any())).thenReturn(List.<Object[]>of(new Object[]{awa.getId(), 3.5}));
        // Awa a mené deux audits comme responsable et un comme auditrice associée ; Ali, un seul.
        when(audits.realisesCommeResponsablesDepuis(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{awa.getId(), 2L}));
        when(audits.realisesCommeMembresDepuis(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{awa.getId(), 1L}, new Object[]{ali.getId(), 1L}));

        List<AuditeurDto> page = serviceAuditeurs.page(new FiltreVivier(), PageRequest.of(0, 10)).getContent();

        assertThat(page).extracting(AuditeurDto::getScoreEvaluationMoyen).containsExactly(3.5, null);
        assertThat(page).extracting(AuditeurDto::getAuditsRealises12Mois).containsExactly(3L, 1L);
        // Trois requêtes pour toute la page, et non deux par auditeur.
        verify(evaluations, never()).moyenne(any());
        verify(audits, never()).realisesDepuis(any(), any());
    }
}
