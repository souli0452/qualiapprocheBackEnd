package com.qualiapproche.workflow.adapter;

import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.model.Cosignataires;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.service.AbstractWorkflowService;
import com.qualiapproche.workflow.service.RolesUtilisateurService;
import com.qualiapproche.workflow.service.StructureUtilisateurService;
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
 * Séparation des signatures : celui qui soumet un dossier ne le décide pas à l'étape qui le nomme
 * parmi ses co-signataires.
 *
 * <p>Le rôle ne savait pas exprimer cette règle. La vérification d'un document est confiée au rôle
 * {@code PILOTE} ; le pilote qui rédige lui-même une procédure porte ce rôle, et se trouvait donc
 * habilité à vérifier son propre dépôt. L'étape nomme donc ses signataires par leur <b>identité</b>,
 * et n'écarte que celui d'entre eux qui a soumis — les autres décident comme avant, ce qu'un retrait
 * de rôle n'aurait pas permis.</p>
 *
 * <p>La règle ne s'active que si l'étape nomme quelqu'un : c'est la garantie que les circuits déjà
 * en service se décident exactement comme avant.</p>
 */
class SeparationDesSignaturesTest {

    private static final String AUTEUR = "3f1b5c20-0000-4000-8000-00000000000a";
    private static final String AUTRE_SIGNATAIRE = "3f1b5c20-0000-4000-8000-00000000000b";

    private RolesUtilisateurService rolesUtilisateurService;
    private WorkflowConditionAdapter adapter;

    @BeforeEach
    void setUp() {
        rolesUtilisateurService = mock(RolesUtilisateurService.class);
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("PILOTE"));
        // Le mock rend null pour la structure de l'appelant : ces tests jugent la séparation des
        // signatures, pas la structure, et le contrôle de structure reste alors sans effet.
        adapter = new WorkflowConditionAdapter(rolesUtilisateurService,
                mock(StructureUtilisateurService.class));
    }

    @AfterEach
    void nettoyerContexte() {
        SecurityContextHolder.clearContext();
    }

    private void authentifier(String userId) {
        Jwt jwt = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .subject(userId)
                .issuedAt(java.time.Instant.EPOCH)
                .expiresAt(java.time.Instant.EPOCH.plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, java.util.List.of()));
    }

    /** Un document déposé par {@code auteur}, tel que le moteur le voit. */
    private ExecutionContext<IWorkflowData> document(String auteur) {
        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(UUID.randomUUID().toString())
                .resourceType("DOCUMENT")
                .createurId(auteur)
                .build();
        ExecutionContext<IWorkflowData> contexte = new ExecutionContext<>();
        contexte.setData(instance);
        return contexte;
    }

    /**
     * Une issue de l'étape de vérification, telle que le catalogue la construit : habilitée au rôle
     * responsable de l'étape, et portant les personnes que cette étape désigne comme signataires.
     */
    private TransitionPersistante verification(String habilitation, String cosignataires) {
        Etat etape = new Etat("42");
        etape.setLibelle("Vérification");
        TransitionPersistante transition = new TransitionPersistante(
                UUID.randomUUID().toString(), etape, new Etat("43"));
        transition.setPermission(habilitation);
        transition.setCosignataires(Cosignataires.lire(cosignataires));
        return transition;
    }

    @Test
    @DisplayName("Sans co-signataire désigné, l'étape se décide comme avant — l'auteur compris")
    void listeVide_comportementInchange() {
        authentifier(AUTEUR);

        // C'est l'état de tous les circuits livrés : la règle est inactive tant que personne n'est
        // nommé, et la mise à jour ne change le sort d'aucun dossier en cours.
        assertThat(adapter.estAutorise(document(AUTEUR), verification("PILOTE", null))).isTrue();
        assertThat(adapter.estAutorise(document(AUTEUR), verification("PILOTE", "  "))).isTrue();
    }

    @Test
    @DisplayName("Le signataire qui a déposé le document ne le vérifie pas lui-même")
    void auteurCosignataire_refuse() {
        authentifier(AUTEUR);

        assertThat(adapter.estAutorise(document(AUTEUR), verification("PILOTE", AUTEUR))).isFalse();
    }

    @Test
    @DisplayName("L'autre signataire de l'étape vérifie ce document : elle n'est fermée qu'à son auteur")
    void autreSignataire_autorise() {
        authentifier(AUTRE_SIGNATAIRE);

        // La différence avec un retrait de rôle est là : l'étape reste décidable, par quelqu'un
        // d'autre. Sans quoi la règle se paierait d'un dossier immobile.
        assertThat(adapter.estAutorise(document(AUTEUR),
                verification("PILOTE", AUTEUR + "," + AUTRE_SIGNATAIRE))).isTrue();
    }

    @Test
    @DisplayName("Un auteur que l'étape ne nomme pas n'est pas visé : il n'y avait rien à séparer")
    void auteurHorsListe_habilitationOrdinaire() {
        authentifier(AUTEUR);

        // L'étape écarte ses signataires ; l'auteur n'en est pas, et l'habilitation ordinaire
        // tranche seule — ici en sa faveur, puisqu'il porte le rôle attendu.
        assertThat(adapter.estAutorise(document(AUTEUR), verification("PILOTE", AUTRE_SIGNATAIRE)))
                .isTrue();
    }

    @Test
    @DisplayName("Toutes les issues de l'étape sont fermées à son auteur, pas seulement l'approbation")
    void etapeEntiere_fermee() {
        authentifier(AUTEUR);

        // Se vérifier soi-même, c'est aussi bien se retourner le document que le transmettre :
        // la séparation porte sur l'étape, pas sur l'une de ses issues.
        assertThat(adapter.estAutorise(document(AUTEUR),
                verification("PILOTE", AUTRE_SIGNATAIRE + " , " + AUTEUR))).isFalse();
    }

    @Test
    @DisplayName("Un identifiant écrit dans une autre casse désigne la même personne")
    void casseIndifferente() {
        authentifier(AUTEUR);

        // Le sujet du jeton et l'identifiant choisi dans l'écran ne traversent pas les mêmes
        // normalisations : une majuscule ne peut pas décider de qui a le droit de signer.
        assertThat(adapter.estAutorise(document(AUTEUR),
                verification("PILOTE", AUTEUR.toUpperCase(java.util.Locale.ROOT)))).isFalse();
    }

    @Test
    @DisplayName("L'administration passe outre : sinon un dossier resterait sans personne pour le décider")
    void administration_passeOutre() {
        authentifier(AUTEUR);
        when(rolesUtilisateurService.rolesDeLUtilisateurCourant()).thenReturn(Set.of("PILOTE", "SUPER_ADMIN"));

        assertThat(adapter.estAutorise(document(AUTEUR), verification("PILOTE", AUTEUR))).isTrue();
    }

    @Test
    @DisplayName("Le refus dit pourquoi : l'auteur porte le rôle attendu, on ne peut pas le lui nier")
    void motifDuRefus_deposeDansLeContexte() {
        authentifier(AUTEUR);
        ExecutionContext<IWorkflowData> contexte = document(AUTEUR);

        adapter.estAutorise(contexte, verification("PILOTE", AUTEUR));

        assertThat(contexte.getParametre(AbstractWorkflowService.CLE_MOTIF_REFUS, String.class))
                .contains("Vous avez soumis ce dossier : l'étape « Vérification » revient à un autre "
                        + "signataire que son auteur.");
    }

    @Test
    @DisplayName("Un dossier sans auteur inscrit ne ferme l'étape à personne")
    void aucunAuteurInscrit_regleSansEffet() {
        authentifier(AUTEUR);

        // Circuits ouverts avant que le créateur ne soit inscrit : rien ne permet de dire qui a
        // soumis, et fermer l'étape à tous serait pire que de la laisser ouverte.
        assertThat(adapter.estAutorise(document(null), verification("PILOTE", AUTEUR))).isTrue();
    }
}
