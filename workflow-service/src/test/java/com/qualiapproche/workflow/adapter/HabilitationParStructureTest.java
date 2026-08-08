package com.qualiapproche.workflow.adapter;

import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.service.RolesUtilisateurService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Habilitation par rôle, bornée à la structure où le dossier se trouve.
 *
 * <p>Un rôle est porté dans toutes les structures : jugée sur le rôle seul, chaque étape s'ouvrait
 * à tous ses porteurs — le pilote d'une structure pouvait décider sur les dossiers de toutes les
 * autres, comme il en recevait les courriels. La structure inscrite sur le dossier — celle du
 * déclarant, ou du dernier transfert — borne désormais la décision comme la notification.</p>
 *
 * <p>Ces tests fixent aussi les dispenses, qui sont autant de portes à ne pas refermer : les rôles
 * à portée globale, les dossiers sans structure inscrite, les jetons qui n'en déclarent pas.</p>
 */
class HabilitationParStructureTest {

    private static final String STRUCTURE_DU_DOSSIER = "structure-7";
    private static final String AUTRE_STRUCTURE = "structure-9";

    private RolesUtilisateurService rolesUtilisateurService;
    private com.qualiapproche.workflow.service.StructureUtilisateurService structureUtilisateurService;
    private WorkflowConditionAdapter adapter;

    @BeforeEach
    void setUp() {
        rolesUtilisateurService = mock(RolesUtilisateurService.class);
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("PILOTE"));
        structureUtilisateurService = mock(com.qualiapproche.workflow.service.StructureUtilisateurService.class);
        adapter = new WorkflowConditionAdapter(rolesUtilisateurService, structureUtilisateurService);
    }

    @AfterEach
    void nettoyerContexte() {
        SecurityContextHolder.clearContext();
    }

    private void authentifier(String structureId) {
        Jwt.Builder jwt = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .subject("utilisateur")
                .issuedAt(java.time.Instant.EPOCH)
                .expiresAt(java.time.Instant.EPOCH.plusSeconds(3600));
        if (structureId != null) {
            jwt.claim("structure_id", structureId);
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt.build(), null, java.util.List.of()));
        // La structure de l'appelant passe par le service — jeton d'abord, user-service à
        // défaut : le royaume Keycloak ne mappe pas l'attribut en claim. Le mock rend ici ce
        // que le jeton dit, comme le fait le service réel.
        when(structureUtilisateurService.structureDeLUtilisateurCourant()).thenReturn(structureId);
    }

    /** Un dossier tel que le moteur le voit : dans sa structure, ou sans structure inscrite. */
    private ExecutionContext<IWorkflowData> dossierDansLaStructure(String structureId) {
        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(UUID.randomUUID().toString())
                .resourceType("NON_CONFORMITE")
                .structureId(structureId)
                .build();
        ExecutionContext<IWorkflowData> contexte = new ExecutionContext<>();
        contexte.setData(instance);
        return contexte;
    }

    private TransitionPersistante transitionReserveeAuRole(String role) {
        TransitionPersistante transition = new TransitionPersistante(
                UUID.randomUUID().toString(), null, null);
        transition.setPermission(role);
        return transition;
    }

    @Test
    @DisplayName("Le porteur du rôle décide dans sa structure")
    void memeStructure_autorise() {
        authentifier(STRUCTURE_DU_DOSSIER);

        assertThat(adapter.estAutorise(dossierDansLaStructure(STRUCTURE_DU_DOSSIER),
                transitionReserveeAuRole("PILOTE"))).isTrue();
    }

    @Test
    @DisplayName("Le même rôle porté dans une autre structure ne décide pas")
    void autreStructure_refuse() {
        authentifier(AUTRE_STRUCTURE);

        // C'est le bug d'origine : le pilote de n'importe quelle structure pouvait décider — et
        // était prévenu — sur les dossiers de toutes les autres.
        assertThat(adapter.estAutorise(dossierDansLaStructure(STRUCTURE_DU_DOSSIER),
                transitionReserveeAuRole("PILOTE"))).isFalse();
    }

    @Test
    @DisplayName("Sans le rôle, la structure ne donne rien : les deux sont exigés")
    void memeStructureSansLeRole_refuse() {
        authentifier(STRUCTURE_DU_DOSSIER);
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("AGENT"));

        assertThat(adapter.estAutorise(dossierDansLaStructure(STRUCTURE_DU_DOSSIER),
                transitionReserveeAuRole("PILOTE"))).isFalse();
    }

    @Test
    @DisplayName("Un dossier sans structure inscrite garde l'ancienne portée : le rôle suffit")
    void dossierSansStructure_roleSeulJuge() {
        authentifier(AUTRE_STRUCTURE);

        // Dossiers antérieurs à la colonne, ou ouverts par un déclarant sans structure : les
        // bloquer les aurait figés — personne n'aurait plus pu décider dessus.
        assertThat(adapter.estAutorise(dossierDansLaStructure(null),
                transitionReserveeAuRole("PILOTE"))).isTrue();
    }

    @Test
    @DisplayName("Un jeton sans structure ne se compare pas à l'inconnu : le rôle suffit")
    void jetonSansStructure_roleSeulJuge() {
        authentifier(null);

        assertThat(adapter.estAutorise(dossierDansLaStructure(STRUCTURE_DU_DOSSIER),
                transitionReserveeAuRole("PILOTE"))).isTrue();
    }

    @Test
    @DisplayName("La responsabilité qualité décide à ses étapes quelle que soit sa structure")
    void responsableQualite_transverse() {
        authentifier(AUTRE_STRUCTURE);
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant())
                .thenReturn(Set.of("RESPONSABLE_QUALITE"));

        // Le rôle est transverse par définition : borné à sa structure, le responsable qualité
        // n'aurait plus validé que les dossiers venant de la sienne.
        assertThat(adapter.estAutorise(dossierDansLaStructure(STRUCTURE_DU_DOSSIER),
                transitionReserveeAuRole("RESPONSABLE_QUALITE"))).isTrue();
    }

    @Test
    @DisplayName("L'administration passe outre la structure comme le reste")
    void administration_passeOutre() {
        authentifier(AUTRE_STRUCTURE);
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("SUPER_ADMIN"));

        assertThat(adapter.estAutorise(dossierDansLaStructure(STRUCTURE_DU_DOSSIER),
                transitionReserveeAuRole("PILOTE"))).isTrue();
    }
}
