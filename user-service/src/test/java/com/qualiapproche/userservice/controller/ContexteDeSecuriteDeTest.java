package com.qualiapproche.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.config.GlobalExceptionHandler;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.userservice.config.auth.KcAccessDenied;
import com.qualiapproche.userservice.config.auth.KcAuthenticationEntryPoint;
import com.qualiapproche.userservice.config.auth.KcSecurityConfig;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import com.qualiapproche.userservice.service.AppRoleService;
import com.qualiapproche.userservice.service.GardeDesHabilitations;
import com.qualiapproche.userservice.service.KcRoleService;
import com.qualiapproche.userservice.service.KcUserService;
import com.qualiapproche.userservice.entities.mappers.KcRoleMapper;
import org.keycloak.admin.client.Keycloak;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Le contexte des tests de sécurité des contrôleurs : la <b>vraie</b> chaîne de filtres de
 * user-service ({@link KcSecurityConfig}), la sécurité par méthode ({@code @PreAuthorize}), le
 * vrai {@code PermissionChecker} et le vrai gestionnaire d'erreurs. Seuls Keycloak et le service des
 * comptes, qui lui parle, sont simulés.
 *
 * <p>Les rôles applicatifs passent par le vrai {@link AppRoleService} et la vraie
 * {@link GardeDesHabilitations}, sur des dépôts simulés : c'est par HTTP que se vérifie le refus
 * d'une élévation de privilèges, avec le statut et le message que l'écran recevra. Les rôles
 * Keycloak aussi : le vrai {@link KcRoleService} y travaille sur un client Keycloak simulé, pour
 * que la réserve au SUPER_ADMIN se constate de bout en bout.</p>
 *
 * <p>Keycloak n'est jamais joint : le décodeur des jetons est différé, et les tests présentent
 * des jetons déjà authentifiés ({@code SecurityMockMvcRequestPostProcessors.jwt()}).</p>
 */
@Configuration
@EnableMethodSecurity
@EnableSpringDataWebSupport
@Import({KcSecurityConfig.class, KcAuthenticationEntryPoint.class, KcAccessDenied.class,
        GlobalExceptionHandler.class, KcUserController.class, KcRoleController.class,
        AppRoleController.class, AppRoleService.class, GardeDesHabilitations.class})
class ContexteDeSecuriteDeTest {

    @Bean
    KcAuthProperties kcAuthProperties() {
        KcAuthProperties proprietes = new KcAuthProperties();
        proprietes.setIssuerUri("http://keycloak.invalide/realms/quali");
        proprietes.setClientId("quali-sira");
        return proprietes;
    }

    /** Sans secret partagé : seules comptent les autorités du jeton, que chaque test choisit. */
    @Bean("perm")
    PermissionChecker perm() {
        return new PermissionChecker();
    }

    @Bean
    KcUserService kcUserService() {
        return mock(KcUserService.class);
    }

    /** Client d'administration simulé : chaque test y lit ce qui aurait été écrit dans Keycloak. */
    @Bean
    Keycloak keycloak() {
        return mock(Keycloak.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    KcRoleService kcRoleService(Keycloak keycloak, AppRoleRepository roles, UserRoleAssignmentRepository affectations,
                                AppRoleService appRoleService, GardeDesHabilitations garde) {
        return new KcRoleService(keycloak, null, new KcRoleMapper(), roles, affectations, appRoleService, garde);
    }

    /**
     * Le realm des services, que Spring injecte par {@code @Value} jusque dans un bean construit à
     * la main : le fixer après coup serait écrasé. Les autres clés absentes restent tolérées.
     */
    @Bean
    static PropertySourcesPlaceholderConfigurer proprietes() {
        PropertySourcesPlaceholderConfigurer configurateur = new PropertySourcesPlaceholderConfigurer();
        Properties valeurs = new Properties();
        valeurs.setProperty("keycloak.realm", "quali");
        configurateur.setProperties(valeurs);
        configurateur.setIgnoreUnresolvablePlaceholders(true);
        return configurateur;
    }

    @Bean
    AppRoleRepository appRoleRepository() {
        return mock(AppRoleRepository.class);
    }

    @Bean
    UserRoleAssignmentRepository userRoleAssignmentRepository() {
        return mock(UserRoleAssignmentRepository.class);
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
