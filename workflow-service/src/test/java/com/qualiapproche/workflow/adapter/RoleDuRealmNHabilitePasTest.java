package com.qualiapproche.workflow.adapter;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.service.ReglagesOrganisation;
import com.qualiapproche.workflow.service.RolesUtilisateurService;
import com.qualiapproche.workflow.service.StructureUtilisateurService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Un rôle du realm Keycloak ne vaut pas habilitation d'étape.
 *
 * <p>Le contrôle se repliait sur les rôles du jeton quand les rôles applicatifs ne contenaient pas
 * celui qu'exige l'étape. Aucun circuit ne désigne de rôle du realm — l'éditeur ne propose que les
 * rôles applicatifs — mais le repli ouvrait l'étape « PILOTE » à quiconque portait dans le realm un
 * rôle de ce nom, sans être pilote pour l'application. Ces tests empêchent son retour.</p>
 */
class RoleDuRealmNHabilitePasTest {

    private RolesUtilisateurService rolesUtilisateurService;
    private WorkflowConditionAdapter adapter;

    @BeforeEach
    void setUp() {
        rolesUtilisateurService = mock(RolesUtilisateurService.class);
        StructureUtilisateurService structures = mock(StructureUtilisateurService.class);
        when(structures.structureDeLUtilisateurCourant()).thenReturn(null);
        adapter = new WorkflowConditionAdapter(rolesUtilisateurService, structures,
                mock(PermissionChecker.class), mock(ReglagesOrganisation.class));
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    /** Un jeton dont le realm porte {@code ROLE_<role>}, comme le pose KeycloakRoleConverter. */
    private void authentifierAvecRoleDuRealm(String role) {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none").subject("utilisateur")
                .issuedAt(java.time.Instant.EPOCH).expiresAt(java.time.Instant.EPOCH.plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                jwt, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private ExecutionContext<IWorkflowData> dossier() {
        ExecutionContext<IWorkflowData> contexte = new ExecutionContext<>();
        contexte.setData(WorkflowValidationInstance.builder().id(UUID.randomUUID())
                .resourceId(UUID.randomUUID().toString()).resourceType("NON_CONFORMITE").build());
        return contexte;
    }

    private TransitionPersistante reserveeA(String role) {
        TransitionPersistante transition = new TransitionPersistante(UUID.randomUUID().toString(), null, null);
        transition.setPermission(role);
        return transition;
    }

    @Test
    @DisplayName("Un rôle du realm homonyme, seul, ne permet pas de décider")
    void roleDuRealmHomonymeSeul_refuse() {
        authentifierAvecRoleDuRealm("PILOTE");
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("AGENT"));

        assertThat(adapter.estAutorise(dossier(), reserveeA("PILOTE"))).isFalse();
    }

    @Test
    @DisplayName("user-service injoignable, le rôle du realm ne prend pas le relais")
    void userServiceMuet_roleDuRealmNeSupplePas() {
        authentifierAvecRoleDuRealm("RESPONSABLE_QUALITE");
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of());

        assertThat(adapter.estAutorise(dossier(), reserveeA("RESPONSABLE_QUALITE"))).isFalse();
    }

    @Test
    @DisplayName("Le rôle applicatif, lui, suffit toujours")
    void roleApplicatif_autorise() {
        authentifierAvecRoleDuRealm("AUCUN");
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("PILOTE"));

        assertThat(adapter.estAutorise(dossier(), reserveeA("PILOTE"))).isTrue();
    }
}
