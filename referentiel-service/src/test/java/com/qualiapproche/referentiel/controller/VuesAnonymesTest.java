package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.referentiel.service.LicenceInstalleeService;
import com.qualiapproche.referentiel.service.StructureService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les deux points d'entrée ouverts sans jeton ne rendent, à l'appel anonyme, que ce que leurs
 * lecteurs anonymes lisent : l'état de la licence pour l'ouverture de session et la passerelle.
 * Le courriel de la direction, son autorité signataire, la référence et le titulaire de la licence
 * restent aux appels authentifiés.
 */
class VuesAnonymesTest {

    private StructureController structures;
    private LicenceController licence;

    @BeforeEach
    void setUp() {
        StructureService structureService = mock(StructureService.class);
        when(structureService.getDirection()).thenReturn(StructureDto.builder()
                .libelleCourt("DQA")
                .email("direction@exemple.bf")
                .autoriteSignataire("M. le Directeur général")
                .titreSignataire("Directeur général")
                .createdById("admin-1")
                .licenceActive(true)
                .licenseDaysRemaining(30L)
                .modulesSubscribed(List.of("NC", "AUDIT"))
                .build());
        structures = new StructureController(structureService);

        LicenceInstalleeService licenceService = mock(LicenceInstalleeService.class);
        when(licenceService.etat()).thenReturn(EtatLicenceDto.builder()
                .statut("ACTIVE").actionsOuvertes(true).type("COMMERCIALE")
                .reference("QS-2026-000148").partenaireNom("Direction Qualité Approche")
                .debut(LocalDate.of(2026, 1, 1)).fin(LocalDate.of(2026, 12, 31)).joursRestants(80)
                .modules(List.of("NC")).utilisateursMax(25).message("Licence active.")
                .build());
        licence = new LicenceController(licenceService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void anonyme() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("cle", "anonyme",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    }

    private void authentifie() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("agent", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));
    }

    @Test
    @DisplayName("Sans jeton, la direction ne livre que l'état de sa licence")
    void direction_anonyme_reduite() {
        anonyme();

        StructureDto vue = structures.getDirection().getBody();

        assertThat(vue.getEmail()).isNull();
        assertThat(vue.getAutoriteSignataire()).isNull();
        assertThat(vue.getTitreSignataire()).isNull();
        assertThat(vue.getCreatedById()).isNull();
        assertThat(vue.getLibelleCourt()).isNull();
        // Ce que l'ouverture de session lit : rien n'en manque.
        assertThat(vue.getLicenceActive()).isTrue();
        assertThat(vue.getLicenseDaysRemaining()).isEqualTo(30L);
        assertThat(vue.getModulesSubscribed()).containsExactly("NC", "AUDIT");
    }

    @Test
    @DisplayName("Sans aucun contexte de sécurité, la vue est la même que pour un anonyme")
    void direction_sansContexte_reduite() {
        assertThat(structures.getDirection().getBody().getEmail()).isNull();
    }

    @Test
    @DisplayName("Authentifié, la direction est rendue en entier, comme avant")
    void direction_authentifiee_complete() {
        authentifie();

        assertThat(structures.getDirection().getBody().getEmail()).isEqualTo("direction@exemple.bf");
    }

    @Test
    @DisplayName("Sans jeton, l'état de la licence garde ce que lit la passerelle, sans référence ni titulaire")
    void licence_anonyme_reduite() {
        anonyme();

        ApiResponse<EtatLicenceDto> reponse = licence.etat().getBody();
        EtatLicenceDto vue = reponse.getData();

        assertThat(vue.getReference()).isNull();
        assertThat(vue.getPartenaireNom()).isNull();
        assertThat(vue.getType()).isNull();
        // Le contrôle de licence de la passerelle et le plafond de comptes n'y perdent rien.
        assertThat(vue.isActionsOuvertes()).isTrue();
        assertThat(vue.getModules()).containsExactly("NC");
        assertThat(vue.getMessage()).isEqualTo("Licence active.");
        assertThat(vue.getUtilisateursMax()).isEqualTo(25);
    }

    @Test
    @DisplayName("Authentifié, l'état de la licence est complet : l'écran de la licence en a besoin")
    void licence_authentifiee_complete() {
        authentifie();

        assertThat(licence.etat().getBody().getData().getReference()).isEqualTo("QS-2026-000148");
    }
}
