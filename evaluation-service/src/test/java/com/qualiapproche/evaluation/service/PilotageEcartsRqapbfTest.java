package com.qualiapproche.evaluation.service;

import com.qualiapproche.evaluation.dto.NotationCritereDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.EvaluationRQAPBF;
import com.qualiapproche.evaluation.entities.NoeudReferentiel;
import com.qualiapproche.evaluation.entities.NotationCritere;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.enumeration.StatutNotation;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.EvaluationRQAPBFRepository;
import com.qualiapproche.evaluation.repository.NoeudReferentielRepository;
import com.qualiapproche.evaluation.repository.NotationCritereRepository;
import com.qualiapproche.evaluation.repository.PlanAuditRepository;
import com.qualiapproche.evaluation.repository.RappelEcheanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les écarts RQAP-BF transmis au module NC (D18) portent ce qui les fonde.
 *
 * <p>L'écran prévoit la preuve et le commentaire de l'évaluateur sous chaque critère ; le service
 * ne les recopiait pas, et ils restaient vides.</p>
 */
class PilotageEcartsRqapbfTest {

    private static final UUID DIRECTION = UUID.randomUUID();

    private final NotationCritereRepository notations = mock(NotationCritereRepository.class);
    private final PilotageAuditService service = new PilotageAuditService(
            mock(AuditRepository.class), mock(AuditService.class), mock(EvaluationRQAPBFRepository.class),
            mock(ConstatRepository.class), notations, mock(PlanAuditRepository.class),
            mock(NoeudReferentielRepository.class), mock(RappelEcheanceRepository.class), regles());

    private static RegleEcartRQAPBFService regles() {
        RegleEcartRQAPBFService regles = mock(RegleEcartRQAPBFService.class);
        when(regles.niveauxEnEcart(any())).thenReturn(java.util.EnumSet.of(
                NiveauNotationRQAPBF.NON_CONFORME, NiveauNotationRQAPBF.A_AMELIORER));
        return regles;
    }

    @BeforeEach
    void connecter() {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject("rq").claim("direction_id", DIRECTION.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Un écart transmis porte la preuve et le commentaire de sa notation")
    void un_ecart_porte_sa_preuve_et_son_commentaire() {
        Audit audit = Audit.builder().reference("AUD-2026-004").build();
        audit.setId(UUID.randomUUID());
        NoeudReferentiel critere = NoeudReferentiel.builder().code("E1.1-C1").libelle("Politique qualité diffusée").build();
        critere.setId(UUID.randomUUID());
        NotationCritere notation = NotationCritere.builder()
                .evaluation(EvaluationRQAPBF.builder().audit(audit).build())
                .noeud(critere)
                .niveau(NiveauNotationRQAPBF.NON_CONFORME)
                .statut(StatutNotation.PUBLIE)
                .preuve("Affichage absent dans les services")
                .commentaire("Vu lors de la visite du 3 octobre")
                .build();
        notation.setId(UUID.randomUUID());
        when(notations.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notation)));

        NotationCritereDto ecart = service.ecartsRqapbf(null, PageRequest.of(0, 10)).getContent().get(0);

        assertThat(ecart.getPreuve()).isEqualTo("Affichage absent dans les services");
        assertThat(ecart.getCommentaire()).isEqualTo("Vu lors de la visite du 3 octobre");
        assertThat(ecart.getAuditReference()).isEqualTo("AUD-2026-004");
    }
}
