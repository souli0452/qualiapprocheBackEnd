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
        // Possible pour un environnement de test. Le joker doit être posé comme MOTIF et non comme origine :
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
    @DisplayName("Le défaut couvre les domaines du produit et le poste de développement")
    void defaut_couvreLesDomainesDuProduit() {
        // C'est la configuration livrée quand aucune variable n'est posée : le frontal déployé doit
        // passer sans réglage, sans quoi la connexion échouerait sur une erreur CORS.
        CorsConfiguration config = configuration("", CorsConfig.MOTIFS_PAR_DEFAUT);

        assertThat(config.checkOrigin("https://app.test.qualisira.com"))
                .isEqualTo("https://app.test.qualisira.com");
        assertThat(config.checkOrigin("http://localhost:4200")).isNotNull();
        assertThat(config.checkOrigin("http://127.0.0.1:4200")).isNotNull();
    }

    @Test
    @DisplayName("Le défaut n'ouvre plus à un site quelconque")
    void defaut_refuseLesSitesEtrangers() {
        // Le défaut était « * » : n'importe quel site pouvait agir au nom d'un utilisateur connecté.
        CorsConfiguration config = configuration("", CorsConfig.MOTIFS_PAR_DEFAUT);

        assertThat(config.getAllowedOriginPatterns()).doesNotContain("*");
        assertThat(config.checkOrigin("https://un-site-quelconque.fr")).isNull();
        assertThat(config.checkOrigin("https://qualisira.com.attaquant.fr")).isNull();
        assertThat(config.checkOrigin("http://app.test.qualisira.com"))
                .as("le frontal déployé n'est servi qu'en HTTPS")
                .isNull();
        assertThat(config.checkOrigin("null")).isNull();
    }

    @Test
    @DisplayName("Le défaut ne fait pas confiance aux autres hôtes des domaines du produit")
    void defaut_refuseLesHotesVoisins() {
        // s3.horeb.tech sert le stockage objet : une page HTML déposée dans un compartiment
        // pourrait, sinon, lire les données de l'utilisateur connecté et écrire en son nom.
        CorsConfiguration config = configuration("", CorsConfig.MOTIFS_PAR_DEFAUT);

        assertThat(config.checkOrigin("https://s3.horeb.tech")).isNull();
        assertThat(config.checkOrigin("https://console.horeb.tech")).isNull();
        assertThat(config.checkOrigin("https://mail.qualisira.com")).isNull();
        assertThat(config.getAllowedOriginPatterns())
                .noneMatch(motif -> motif.startsWith("https://*"));
    }

    @Test
    @DisplayName("Le défaut n'admet que le frontal de test, pas les autres hôtes de test.qualisira.com")
    void defaut_refuseLesAutresHotesDeTest() {
        // Seul app.test.qualisira.com sert l'application. Le domaine de test lui-même, la passerelle
        // et les noms cités naguère (qui ne résolvent pas) n'ont pas à écrire dans l'API ; le
        // domaine nu qualisira.com sert le site vitrine.
        CorsConfiguration config = configuration("", CorsConfig.MOTIFS_PAR_DEFAUT);

        assertThat(config.checkOrigin("https://test.qualisira.com")).isNull();
        assertThat(config.checkOrigin("https://qualisira.test.qualisira.com")).isNull();
        assertThat(config.checkOrigin("https://api-gateway.test.qualisira.com")).isNull();
        assertThat(config.checkOrigin("https://app.test.qualisira.com.attaquant.fr")).isNull();
        assertThat(config.checkOrigin("https://qualisira.horeb.tech")).isNull();
        assertThat(config.checkOrigin("https://qualisira.com")).isNull();
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
