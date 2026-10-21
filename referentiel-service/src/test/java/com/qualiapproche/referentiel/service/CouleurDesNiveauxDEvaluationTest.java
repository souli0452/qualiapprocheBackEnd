package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.NiveauEvaluationAuditeur;
import com.qualiapproche.referentiel.entities.mappers.NiveauEvaluationAuditeurMapperImpl;
import com.qualiapproche.referentiel.repository.NiveauEvaluationAuditeurRepository;
import com.qualiapproche.referentiel.service.impl.NiveauEvaluationAuditeurService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Chaque niveau de l'échelle d'évaluation des auditeurs porte sa couleur.
 *
 * <p>Elle part telle quelle dans le style des écrans : seule la forme {@code #RRGGBB} est admise.</p>
 */
class CouleurDesNiveauxDEvaluationTest {

    private final NiveauEvaluationAuditeurRepository depot = mock(NiveauEvaluationAuditeurRepository.class);
    private final NiveauEvaluationAuditeurService service =
            new NiveauEvaluationAuditeurService(depot, new NiveauEvaluationAuditeurMapperImpl());

    @BeforeEach
    void connecter() {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject("admin").claim("direction_id", UUID.randomUUID().toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(depot.save(any(NiveauEvaluationAuditeur.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private static NiveauEvaluationAuditeurDto niveau(String couleur) {
        return NiveauEvaluationAuditeurDto.builder().libelle("Satisfaisant").ordre(3).couleur(couleur).build();
    }

    @Test
    @DisplayName("La couleur choisie est gardée, en minuscules")
    void la_couleur_est_gardee() {
        assertThat(service.creer(niveau("#16A34A")).getCouleur()).isEqualTo("#16a34a");
    }

    @Test
    @DisplayName("Sans couleur, le niveau n'en porte pas : l'écran la déduit de son rang")
    void sans_couleur() {
        assertThat(service.creer(niveau("  ")).getCouleur()).isNull();
        assertThat(service.creer(niveau(null)).getCouleur()).isNull();
    }

    @Test
    @DisplayName("Une couleur hors de la forme #RRGGBB est refusée, rien n'est enregistré")
    void une_couleur_mal_formee_est_refusee() {
        for (String fausse : new String[]{"red", "#16a34", "#16a34a; background:url(x)", "rgb(0,0,0)"}) {
            assertThatThrownBy(() -> service.creer(niveau(fausse)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        verify(depot, never()).save(any(NiveauEvaluationAuditeur.class));
    }
}
