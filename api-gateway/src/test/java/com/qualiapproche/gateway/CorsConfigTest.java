package com.qualiapproche.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les origines que la passerelle accepte, et d'où elles viennent.
 *
 * <p>Elles étaient écrites en dur : changer le domaine du frontal imposait de reconstruire l'image,
 * et jusque-là la connexion échouait sur une erreur qui parle de CORS sans jamais nommer l'origine
 * attendue. C'est ce qui s'est produit au passage sur {@code test}.</p>
 */
class CorsConfigTest {

    private CorsConfiguration configuration(String origines, String motifs) {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "origines", origines);
        ReflectionTestUtils.setField(config, "motifsOrigines", motifs);
        return config.configuration();
    }

    @Test
    @DisplayName("Les origines exactes sont reprises telles quelles")
    void originesExactes_repriseS() {
        CorsConfiguration config = configuration(
                "https://qualisira.test.qualisira.com,http://localhost:4200", "");

        assertThat(config.getAllowedOrigins())
                .containsExactly("https://qualisira.test.qualisira.com", "http://localhost:4200");
    }

    @Test
    @DisplayName("Les espaces d'une saisie à la main ne produisent pas d'origine fantôme")
    void espaces_retires() {
        // Ces valeurs se posent dans une console de déploiement, où une espace après une virgule ne
        // se voit pas — et produirait une origine que rien ne reconnaîtrait jamais.
        CorsConfiguration config = configuration(
                " https://a.qualisira.com , https://b.qualisira.com ,, ", "");

        assertThat(config.getAllowedOrigins())
                .containsExactly("https://a.qualisira.com", "https://b.qualisira.com");
    }

    @Test
    @DisplayName("Un motif couvre les sous-domaines d'un environnement")
    void motif_couvreLesSousDomaines() {
        CorsConfiguration config = configuration("", "https://*.qualisira.com");

        // C'est la vérification qu'opère le navigateur : le motif doit reconnaître le frontal
        // déployé, quel que soit le nombre de niveaux de son sous-domaine.
        assertThat(config.checkOrigin("https://qualisira.test.qualisira.com"))
                .isEqualTo("https://qualisira.test.qualisira.com");
        assertThat(config.checkOrigin("https://api-gateway.test.qualisira.com"))
                .isEqualTo("https://api-gateway.test.qualisira.com");
    }

    @Test
    @DisplayName("Une origine étrangère reste refusée malgré le motif")
    void origineEtrangere_refusee() {
        CorsConfiguration config = configuration("", "https://*.qualisira.com");

        assertThat(config.checkOrigin("https://qualisira.com.attaquant.fr")).isNull();
        assertThat(config.checkOrigin("http://qualisira.test.qualisira.com")).isNull();
    }

    @Test
    @DisplayName("Les identifiants sont autorisés : l'authentification passe par un cookie")
    void identifiants_autorises() {
        CorsConfiguration config = configuration("http://localhost:4200", "");

        // Sans cela, le navigateur n'envoie aucun cookie et toute requête arrive anonyme.
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("Le motif « * » ouvre à toutes les origines, cookies compris")
    void jokerEnMotif_ouvreATous() {
        // C'est la configuration livrée. Le joker doit être posé comme MOTIF et non comme origine :
        // la norme refuse « * » comme origine dès que la requête porte des identifiants, et le
        // navigateur rejette alors la réponse — un échec impossible à distinguer d'un blocage CORS
        // ordinaire.
        CorsConfiguration config = configuration("", "*");

        assertThat(config.getAllowedOrigins())
                .as("« * » posé ici casserait l'authentification par cookie")
                .isNull();
        assertThat(config.getAllowCredentials()).isTrue();

        // Spring renvoie l'origine réellement appelante à la place du joker, ce que la norme
        // accepte avec des identifiants.
        assertThat(config.checkOrigin("https://qualisira.test.qualisira.com"))
                .isEqualTo("https://qualisira.test.qualisira.com");
        assertThat(config.checkOrigin("http://localhost:4200"))
                .isEqualTo("http://localhost:4200");
        assertThat(config.checkOrigin("https://un-site-quelconque.fr"))
                .isEqualTo("https://un-site-quelconque.fr");
    }

    @Test
    @DisplayName("Nommer des origines exactes referme ce que le joker ouvrait")
    void originesNommees_refermentLOuverture() {
        // Le chemin de sortie, pour la production : on nomme le frontal, on vide les motifs.
        CorsConfiguration config = configuration("https://qualisira.test.qualisira.com", "");

        assertThat(config.checkOrigin("https://qualisira.test.qualisira.com"))
                .isEqualTo("https://qualisira.test.qualisira.com");
        assertThat(config.checkOrigin("https://un-site-quelconque.fr")).isNull();
    }

    @Test
    @DisplayName("Sans aucune origine ni motif, rien n'est autorisé")
    void aucuneOrigine_toutRefuse() {
        // Le cas se signale au démarrage par une erreur dans le journal : une passerelle qui
        // n'autorise personne se découvrirait sinon écran par écran.
        CorsConfiguration config = configuration("", "");

        assertThat(config.getAllowedOrigins()).isNull();
        assertThat(config.getAllowedOriginPatterns()).isNull();
        assertThat(config.checkOrigin("http://localhost:4200")).isNull();
    }
}
