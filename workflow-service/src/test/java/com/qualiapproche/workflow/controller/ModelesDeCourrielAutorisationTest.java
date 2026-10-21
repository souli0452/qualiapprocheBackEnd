package com.qualiapproche.workflow.controller;

import com.qualiapproche.common.config.PermissionConfig;
import com.qualiapproche.common.dto.EmailTemplateDto;
import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.service.EmailTemplateMapper;
import com.qualiapproche.workflow.service.EmailTemplateService;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Protection des modèles de courriel d'étape.
 *
 * <p>Ces gabarits sont évalués par le serveur puis envoyés sous le nom de l'organisation. Ils
 * étaient modifiables par tout compte authentifié : ces tests vérifient que seule l'administration
 * des circuits ({@code workflow-write}) les écrit, et que la lecture reste ouverte à l'écran de
 * paramétrage ({@code workflow-read}).</p>
 */
@SpringJUnitConfig(ModelesDeCourrielAutorisationTest.ConfigurationDeTest.class)
class ModelesDeCourrielAutorisationTest {

    @Configuration
    @EnableMethodSecurity
    @Import(PermissionConfig.class)
    static class ConfigurationDeTest {

        @Bean EmailTemplateRepository emailTemplateRepository() { return Mockito.mock(EmailTemplateRepository.class); }

        @Bean
        EmailTemplateService emailTemplateService(EmailTemplateRepository pDepot) {
            return new EmailTemplateService(pDepot, new EmailTemplateMapper());
        }

        @Bean
        EmailTemplateController emailTemplateController(EmailTemplateService pService) {
            return new EmailTemplateController(pService);
        }
    }

    @Autowired private EmailTemplateController controller;
    @Autowired private EmailTemplateRepository depot;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Mockito.reset(depot);
        EmailTemplate modele = EmailTemplate.builder().code("validationRq").subject("Validation")
                .body("<p>corps</p>").build();
        modele.setId(id);
        when(depot.findById(id)).thenReturn(Optional.of(modele));
        when(depot.existsById(id)).thenReturn(true);
        when(depot.findByCode(any())).thenReturn(Optional.empty());
        when(depot.findAll()).thenReturn(List.of(modele));
        when(depot.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authentifierAvec(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agent-qualite", "n/a",
                        Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }

    private EmailTemplateDto modeleSaisi() {
        return EmailTemplateDto.builder().code("autreCode").subject("Objet").body("<p>x</p>").build();
    }

    // ------------------------------------------------------------------ refus

    @Test
    @DisplayName("Sans workflow-write, on ne crée pas de modèle de courriel")
    void creation_sansPermission_refusee() {
        authentifierAvec("workflow-read");

        assertThatThrownBy(() -> controller.createTemplate(modeleSaisi()))
                .isInstanceOf(AccessDeniedException.class);
        verify(depot, never()).save(any());
    }

    @Test
    @DisplayName("Sans workflow-write, on ne réécrit pas un modèle de courriel")
    void modification_sansPermission_refusee() {
        authentifierAvec("nc-write");

        assertThatThrownBy(() -> controller.updateTemplate(id, modeleSaisi()))
                .isInstanceOf(AccessDeniedException.class);
        verify(depot, never()).save(any());
    }

    @Test
    @DisplayName("Sans workflow-write, on ne supprime pas un modèle de courriel")
    void suppression_sansPermission_refusee() {
        authentifierAvec("workflow-read");

        assertThatThrownBy(() -> controller.deleteTemplate(id))
                .isInstanceOf(AccessDeniedException.class);
        verify(depot, never()).deleteById(any());
    }

    @Test
    @DisplayName("Un compte sans permission du paramétrage des circuits ne lit pas les modèles")
    void lecture_sansPermission_refusee() {
        authentifierAvec("nc-read");

        assertThatThrownBy(() -> controller.getAllTemplates()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.getTemplateById(id)).isInstanceOf(AccessDeniedException.class);
    }

    // ------------------------------------------------------------------ autorisation

    @Test
    @DisplayName("workflow-write crée, modifie et supprime un modèle")
    void administration_avecWorkflowWrite_autorisee() {
        authentifierAvec("workflow-write");

        controller.createTemplate(modeleSaisi());
        controller.updateTemplate(id, modeleSaisi());
        controller.deleteTemplate(id);

        verify(depot, Mockito.times(2)).save(any());
        verify(depot).deleteById(id);
    }

    @Test
    @DisplayName("workflow-read suffit à lire les modèles : l'éditeur de circuit les propose")
    void lecture_avecWorkflowRead_autorisee() {
        authentifierAvec("workflow-read");

        assertThat(controller.getAllTemplates().getBody()).hasSize(1);
        assertThat(controller.getTemplateById(id).getBody()).isNotNull();
    }
}
