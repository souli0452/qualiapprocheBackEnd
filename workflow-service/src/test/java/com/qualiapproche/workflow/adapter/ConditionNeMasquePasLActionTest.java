package com.qualiapproche.workflow.adapter;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.model.FaitsDuDossier;
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
 * Une condition non remplie ne fait pas disparaître l'action : elle en motive le refus.
 *
 * <p>La condition métier se jugeait dans {@code WorkflowConditionAdapter}, au même endroit que
 * l'habilitation. Les deux refus se confondaient donc en un seul : l'action était retirée des
 * transitions possibles, et l'écran — qui dessine ses boutons à partir de cette liste — n'en
 * montrait aucun. Le responsable qualité voyait un dossier arrêté, sans bouton, sans message, et
 * sans rien qui lui dise qu'il attendait le solde des actions correctives.</p>
 *
 * <p>Or les deux causes n'ont rien de commun. « Vous n'avez pas le droit » se prouve en regardant
 * l'appelant ; « le dossier n'est pas prêt » se prouve en regardant le dossier, et se lève en
 * agissant sur lui. La première justifie qu'on n'offre rien ; la seconde justifie qu'on offre en
 * expliquant.</p>
 */
class ConditionNeMasquePasLActionTest {

    private static final String PILOTE = "utilisateur-pilote";
    private static final String CONDITION = "PLANS_ACTION_SOLDES";

    private WorkflowConditionAdapter adapter;

    @BeforeEach
    void setUp() {
        RolesUtilisateurService roles = mock(RolesUtilisateurService.class);
        when(roles.rolesDeLUtilisateurCourant()).thenReturn(Set.of("PILOTE"));
        adapter = new WorkflowConditionAdapter(roles,
                mock(com.qualiapproche.workflow.service.StructureUtilisateurService.class),
                mock(PermissionChecker.class),
                mock(com.qualiapproche.workflow.service.ReglagesOrganisation.class));
        authentifier();
    }

    @AfterEach
    void nettoyerContexte() {
        SecurityContextHolder.clearContext();
    }

    private void authentifier() {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none").subject(PILOTE)
                .issuedAt(java.time.Instant.EPOCH)
                .expiresAt(java.time.Instant.EPOCH.plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, java.util.List.of()));
    }

    /** Un dossier portant, ou non, le fait que la transition exigera. */
    private ExecutionContext<IWorkflowData> dossier(String... faits) {
        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(UUID.randomUUID().toString())
                .resourceType("NON_CONFORMITE")
                .faits(FaitsDuDossier.ecrire(Set.of(faits)))
                .build();
        ExecutionContext<IWorkflowData> contexte = new ExecutionContext<>();
        contexte.setData(instance);
        return contexte;
    }

    private TransitionPersistante transition(String conditionRequise) {
        TransitionPersistante transition =
                new TransitionPersistante("1", new Etat("8"), new Etat("9"));
        transition.setLibelle("Clôturer la NC");
        transition.setPermission("PILOTE");
        transition.setConditionRequise(conditionRequise);
        return transition;
    }

    @Test
    @DisplayName("L'action reste offerte à qui est habilité, même si le dossier ne la remplit pas")
    void conditionNonRemplie_actionOfferteQuandMeme() {
        assertThat(adapter.estAutorise(dossier(), transition(CONDITION))).isTrue();
    }

    @Test
    @DisplayName("Le dossier qui remplit la condition l'offre aussi, évidemment")
    void conditionRemplie_actionOfferte() {
        assertThat(adapter.estAutorise(dossier(CONDITION), transition(CONDITION))).isTrue();
    }

    @Test
    @DisplayName("Sans l'habilitation, l'action n'est pas offerte — condition remplie ou non")
    void sansHabilitation_actionRetiree() {
        RolesUtilisateurService sansRole = mock(RolesUtilisateurService.class);
        when(sansRole.rolesDeLUtilisateurCourant()).thenReturn(Set.of("AGENT"));
        WorkflowConditionAdapter strict = new WorkflowConditionAdapter(sansRole,
                mock(com.qualiapproche.workflow.service.StructureUtilisateurService.class),
                mock(PermissionChecker.class),
                mock(com.qualiapproche.workflow.service.ReglagesOrganisation.class));

        // C'est bien l'appelant qui est refusé ici, et lui seul : le dossier, lui, est prêt.
        assertThat(strict.estAutorise(dossier(CONDITION), transition(CONDITION))).isFalse();
    }

    @Test
    @DisplayName("Une transition sans condition se juge comme avant")
    void sansCondition_inchange() {
        assertThat(adapter.estAutorise(dossier(), transition(null))).isTrue();
    }
}
