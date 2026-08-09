package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rétablissement des gabarits livrés au démarrage.
 *
 * <p>Les corps des courriels vivent en base dès le premier lancement, et l'initialisation ne fait
 * qu'ajouter les manquants : une refonte des gabarits n'atteint donc jamais une installation déjà
 * démarrée. Le drapeau lève cette réserve — mais il écrase ce qu'un administrateur a pu rédiger,
 * aussi son inertie par défaut est-elle la propriété à tenir la première.</p>
 *
 * <p>Le dépôt est simulé par une table en mémoire plutôt que par des réponses figées : les quinze
 * modèles livrés sont parcourus à chaque démarrage, et un dépôt qui rendrait la même ligne pour
 * tous les codes ferait passer un test que la réalité contredirait.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RafraichissementDesGabaritsTest {

    /** Nombre de modèles livrés avec la version. */
    private static final int LIVRES = 15;

    /** Un code présent dans la liste livrée, et le fichier qui l'accompagne. */
    private static final String CODE = "validationRq";

    @Mock private EmailTemplateRepository repository;

    private final Map<String, EmailTemplate> base = new HashMap<>();
    private EmailTemplateDataInitializer initialiseur;

    @BeforeEach
    void setUp() {
        when(repository.findByCode(anyString()))
                .thenAnswer(appel -> Optional.ofNullable(base.get(appel.getArgument(0, String.class))));
        when(repository.save(any())).thenAnswer(appel -> {
            EmailTemplate enregistre = appel.getArgument(0);
            base.put(enregistre.getCode(), enregistre);
            return enregistre;
        });
        initialiseur = new EmailTemplateDataInitializer(repository);
    }

    @Test
    @DisplayName("Base vierge : les quinze modèles livrés sont posés")
    void baseViergeLesModelesSontPoses() throws Exception {
        drapeau(false);

        initialiseur.run();

        assertThat(base).hasSize(LIVRES);
        assertThat(base.get(CODE).getBody()).contains("max-width:600px");
    }

    @Test
    @DisplayName("Par défaut, un gabarit déjà en base n'est pas retouché")
    void sansDrapeauLeGabaritEnBaseEstIntact() throws Exception {
        drapeau(false);
        base.put(CODE, retouche());

        initialiseur.run();

        assertThat(base.get(CODE).getBody())
                .as("le corps rédigé sur place doit survivre au démarrage")
                .isEqualTo("<p>corps maison</p>");
        assertThat(base.get(CODE).getSubject()).isEqualTo("Objet réécrit par l'administrateur");
        // Les quatorze autres, eux, manquaient : ils sont bien ajoutés.
        assertThat(base).hasSize(LIVRES);
    }

    @Test
    @DisplayName("Drapeau levé : le gabarit retouché est rétabli dans sa version livrée")
    void avecDrapeauLeGabaritEstRetabli() throws Exception {
        drapeau(true);
        base.put(CODE, retouche());

        initialiseur.run();

        assertThat(base.get(CODE).getBody())
                .as("le corps livré doit avoir remplacé celui de la base")
                .isEqualTo(fichierLivre(CODE));
        assertThat(base.get(CODE).getSubject())
                .isEqualTo("Validation attendue - Non-Conformité {numeroNc}");
    }

    @Test
    @DisplayName("Drapeau laissé actif : un second démarrage n'écrit plus rien")
    void drapeauLaisseActifNEcritPasDeuxFois() throws Exception {
        drapeau(true);
        initialiseur.run();
        clearInvocations(repository);

        // Un drapeau resté levé ne doit pas repousser les quinze lignes à chaque démarrage, ni
        // annoncer au journal une réécriture qui n'a pas eu lieu.
        initialiseur.run();

        verify(repository, never()).save(any());
        assertThat(base).hasSize(LIVRES);
    }

    private void drapeau(boolean actif) {
        ReflectionTestUtils.setField(initialiseur, "rafraichirAuDemarrage", actif);
    }

    private EmailTemplate retouche() {
        return EmailTemplate.builder()
                .code(CODE)
                .subject("Objet réécrit par l'administrateur")
                .body("<p>corps maison</p>")
                .description("Rédigé sur place")
                .build();
    }

    private String fichierLivre(String code) throws IOException {
        return StreamUtils.copyToString(
                new ClassPathResource("templates/" + code + ".html").getInputStream(),
                StandardCharsets.UTF_8);
    }
}
