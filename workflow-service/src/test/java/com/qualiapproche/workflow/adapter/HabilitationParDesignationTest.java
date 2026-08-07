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

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Habilitations qui désignent une <b>personne</b> et non un rôle : le créateur, le titulaire.
 *
 * <p>Un rôle est collectif. Certaines étapes ne se décident pourtant que par une personne précise —
 * soumettre sa propre déclaration, traiter la non-conformité qu'on s'est vu imputer — et les nommer
 * par un rôle ouvrait l'étape à tous ceux qui pouvaient le porter, sur tous les dossiers.</p>
 *
 * <p>Ces tests fixent surtout ce qui distingue les deux désignations : le <b>titulaire se déplace</b>
 * — l'imputation le réinscrit — tandis que le <b>créateur ne change jamais</b>. C'est ce qui permet à
 * une étape retraversée, celle où revient un dossier renvoyé à son auteur, de lui rester ouverte au
 * troisième aller-retour comme au premier.</p>
 *
 * <p>Aucun code d'étape n'apparaît ici, et c'est volontaire : l'habilitation est une donnée de
 * configuration, portée par l'étape ou la transition. Le moteur ne connaît pas « la soumission ».</p>
 */
class HabilitationParDesignationTest {

    private static final String CREATEUR = "utilisateur-createur";
    private static final String TITULAIRE = "utilisateur-titulaire";
    private static final String TIERS = "utilisateur-tiers";

    private RolesUtilisateurService rolesUtilisateurService;
    private WorkflowConditionAdapter adapter;

    @BeforeEach
    void setUp() {
        rolesUtilisateurService = mock(RolesUtilisateurService.class);
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("AGENT"));
        adapter = new WorkflowConditionAdapter(rolesUtilisateurService);
    }

    @AfterEach
    void nettoyerContexte() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private void authentifier(String userId) {
        org.springframework.security.oauth2.jwt.Jwt jwt =
                org.springframework.security.oauth2.jwt.Jwt.withTokenValue("jeton")
                        .header("alg", "none")
                        .subject(userId)
                        .issuedAt(java.time.Instant.EPOCH)
                        .expiresAt(java.time.Instant.EPOCH.plusSeconds(3600))
                        .build();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        jwt, null, java.util.List.of()));
    }

    /** Un dossier tel que le moteur le voit : son créateur, et son titulaire s'il en a un. */
    private ExecutionContext<IWorkflowData> dossier(String createur, String titulaire) {
        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(UUID.randomUUID().toString())
                .resourceType("NON_CONFORMITE")
                .createurId(createur)
                .titulaireId(titulaire)
                .build();
        ExecutionContext<IWorkflowData> contexte = new ExecutionContext<>();
        contexte.setData(instance);
        return contexte;
    }

    /** Une transition habilitée par la désignation passée — telle que la configuration l'écrit. */
    private TransitionPersistante transition(String habilitation) {
        TransitionPersistante transition = new TransitionPersistante(
                UUID.randomUUID().toString(), null, null);
        transition.setPermission(habilitation);
        return transition;
    }

    // ------------------------------------------------------------------ le créateur

    @Test
    @DisplayName("L'auteur du dossier peut franchir une transition réservée à son créateur")
    void createur_autorise() {
        authentifier(CREATEUR);

        assertThat(adapter.estAutorise(dossier(CREATEUR, null), transition("@CREATEUR"))).isTrue();
    }

    @Test
    @DisplayName("Un collègue du même rôle ne peut pas soumettre le dossier d'un autre")
    void tiers_refuse() {
        authentifier(TIERS);

        // Le tiers porte bien le rôle AGENT : c'est précisément ce qu'un rôle ne sait pas exprimer.
        assertThat(adapter.estAutorise(dossier(CREATEUR, null), transition("@CREATEUR"))).isFalse();
    }

    @Test
    @DisplayName("Le créateur garde la main même après qu'un titulaire a été désigné ailleurs")
    void createur_resteAutorise_apresImputation() {
        authentifier(CREATEUR);

        // C'est le cas du dossier renvoyé à son auteur après imputation : réservée au titulaire,
        // l'étape ne lui aurait plus jamais été ouverte, et le dossier serait resté immobile.
        assertThat(adapter.estAutorise(dossier(CREATEUR, TITULAIRE), transition("@CREATEUR"))).isTrue();
    }

    @Test
    @DisplayName("Le titulaire n'hérite pas des transitions réservées au créateur")
    void titulaire_nEstPasLeCreateur() {
        authentifier(TITULAIRE);

        assertThat(adapter.estAutorise(dossier(CREATEUR, TITULAIRE), transition("@CREATEUR"))).isFalse();
    }

    @Test
    @DisplayName("Un dossier sans créateur inscrit n'ouvre la transition à personne")
    void aucunCreateurInscrit_refuse() {
        authentifier(CREATEUR);

        // Mieux vaut un blocage — que l'administration lève — qu'une étape ouverte à tous.
        assertThat(adapter.estAutorise(dossier(null, null), transition("@CREATEUR"))).isFalse();
    }

    @Test
    @DisplayName("L'administration passe outre : c'est ainsi qu'on débloque le dossier d'un partant")
    void administration_passeOutre() {
        authentifier(TIERS);
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("SUPER_ADMIN"));

        assertThat(adapter.estAutorise(dossier(CREATEUR, null), transition("@CREATEUR"))).isTrue();
    }

    // ------------------------------------------------------------------ les deux désignations

    @Test
    @DisplayName("Créateur et titulaire sont deux désignations distinctes sur un même dossier")
    void deuxDesignationsDistinctes() {
        // Le dossier a été déclaré par l'un, imputé à l'autre : chaque étape ouvre à qui elle nomme.
        ExecutionContext<IWorkflowData> dossier = dossier(CREATEUR, TITULAIRE);

        authentifier(TITULAIRE);
        assertThat(adapter.estAutorise(dossier, transition("@TITULAIRE"))).isTrue();
        assertThat(adapter.estAutorise(dossier, transition("@CREATEUR"))).isFalse();

        authentifier(CREATEUR);
        assertThat(adapter.estAutorise(dossier, transition("@CREATEUR"))).isTrue();
        assertThat(adapter.estAutorise(dossier, transition("@TITULAIRE"))).isFalse();
    }

    @Test
    @DisplayName("La désignation se reconnaît quelle que soit la casse écrite dans le circuit")
    void casseToleree() {
        authentifier(CREATEUR);

        assertThat(adapter.estAutorise(dossier(CREATEUR, null), transition("@createur"))).isTrue();
    }

    @Test
    @DisplayName("Une habilitation par rôle reste jugée sur le rôle, sans considérer les désignations")
    void habilitationParRole_inchangee() {
        authentifier(TIERS);
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("AGENT"));

        // Le tiers n'est ni créateur ni titulaire : une étape ouverte au rôle AGENT lui reste ouverte.
        assertThat(adapter.estAutorise(dossier(CREATEUR, TITULAIRE), transition("AGENT"))).isTrue();
    }
}
