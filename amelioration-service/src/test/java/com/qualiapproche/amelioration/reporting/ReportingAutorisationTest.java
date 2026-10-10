package com.qualiapproche.amelioration.reporting;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.reporting.config.ReportConfigService;
import com.qualiapproche.amelioration.reporting.controller.ReportingController;
import com.qualiapproche.amelioration.reporting.dto.EReportType;
import com.qualiapproche.amelioration.reporting.dto.ReportingInputDto;
import com.qualiapproche.amelioration.reporting.dto.ReportingResponseDto;
import com.qualiapproche.amelioration.reporting.service.ReportingService;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.service.NonConformiteService;
import com.qualiapproche.common.config.PermissionConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'état imprimé d'une non-conformité.
 *
 * <p>Le point d'entrée n'avait aucune garde et servait l'état de n'importe quel dossier à qui en
 * citait l'identifiant. Ces tests fixent les deux verrous : la permission de lecture des
 * non-conformités, et la règle de visibilité de la fiche appliquée au dossier demandé.</p>
 */
@SpringJUnitConfig(ReportingAutorisationTest.ConfigurationDeTest.class)
class ReportingAutorisationTest {

    @Configuration
    @EnableMethodSecurity
    @Import(PermissionConfig.class)
    static class ConfigurationDeTest {

        @Bean NonConformiteRepository nonConformiteRepository() { return Mockito.mock(NonConformiteRepository.class); }
        @Bean NonConformiteService nonConformiteService() { return Mockito.mock(NonConformiteService.class); }
        @Bean ReportConfigService reportConfigService() { return Mockito.mock(ReportConfigService.class); }

        @Bean
        ReportingService reportingService(ReportConfigService pConfig, NonConformiteRepository pDepot,
                                          NonConformiteService pNc) {
            return new ReportingService(pConfig, pConfig, pDepot, pNc);
        }

        @Bean
        ReportingController reportingController(ReportingService pService) {
            return new ReportingController(pService);
        }
    }

    @Autowired private ReportingController controller;
    @Autowired private NonConformiteRepository depot;
    @Autowired private NonConformiteService nonConformiteService;
    @Autowired private ReportConfigService configService;

    private final UUID dossierDAilleurs = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Mockito.reset(depot, nonConformiteService, configService);
        NonConformite nc = new NonConformite();
        nc.setId(dossierDAilleurs);
        nc.setStructureSoumissionId("DQA");
        nc.setCreatedAt(java.time.LocalDateTime.of(2026, 3, 2, 9, 30));
        nc.setDateVisaEmetteur(java.time.LocalDateTime.of(2026, 3, 3, 9, 30));
        nc.setPlanActions(new java.util.ArrayList<>());
        when(depot.findById(dossierDAilleurs)).thenReturn(Optional.of(nc));
        when(configService.buildReportNf(any(), any(), any()))
                .thenReturn(new ReportingResponseDto(new byte[]{1}));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authentifierAvec(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agent-gsi", "n/a",
                        Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }

    private ReportingInputDto demande() {
        return ReportingInputDto.builder().reportType(EReportType.NON_CONFORMITE).entityId(dossierDAilleurs).build();
    }

    @Test
    @DisplayName("Sans permission de lecture des non-conformités, l'état est refusé")
    void sansPermission_refuse() {
        authentifierAvec("document-read");

        assertThatThrownBy(() -> controller.generateReport(demande())).isInstanceOf(AccessDeniedException.class);
        verify(depot, never()).findById(any());
    }

    @Test
    @DisplayName("Le dossier d'une autre structure est introuvable, même avec la permission")
    void dossierDUneAutreStructure_introuvable() throws Exception {
        authentifierAvec("nc-read");
        when(nonConformiteService.peutConsulter(any())).thenReturn(false);

        assertThatThrownBy(() -> controller.generateReport(demande()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verify(configService, never()).buildReportNf(any(), any(), any());
    }

    @Test
    @DisplayName("Un dossier consultable s'imprime")
    void dossierConsultable_imprime() throws Exception {
        authentifierAvec("nc-read");
        when(nonConformiteService.peutConsulter(any())).thenReturn(true);

        assertThat(controller.generateReport(demande()).getBody()).containsExactly(1);
    }
}
