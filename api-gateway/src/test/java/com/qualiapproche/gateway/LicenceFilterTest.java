package com.qualiapproche.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Le contrôle de licence à l'entrée de la plateforme.
 *
 * <p>Ce qui se joue ici est ce qui manquait entièrement au dispositif précédent : un appel HTTP
 * direct sur une ressource d'un module jamais acheté aboutissait, licence expirée comprise. Le
 * navigateur était le seul point de contrôle.</p>
 *
 * <p>Deux erreurs seraient graves en sens inverse : fermer la consultation — qui prendrait les
 * données du client en otage — et fermer l'installation d'une licence, qui rendrait une
 * installation expirée définitivement irrécupérable.</p>
 */
class LicenceFilterTest {

    private EtatLicenceCache cache;
    private LicenceFilter filtre;
    private GatewayFilterChain suite;
    private AtomicBoolean laissePasser;

    @BeforeEach
    void setUp() {
        cache = mock(EtatLicenceCache.class);
        filtre = new LicenceFilter(cache);

        Map<String, String> modules = new LinkedHashMap<>();
        modules.put("/amelioration-service/api/v1/reclamation", "RECLAMATION");
        modules.put("/amelioration-service/api/v1/risque", "RISQUE");
        modules.put("/amelioration-service", "NON_CONFORMITE");
        modules.put("/support-service", "DOCUMENTAIRE");
        filtre.setModules(modules);
        filtre.setExemptions(List.of("/user-service/api/v1/login",
                "/user-service/api/v1/initiate-reset-pwd",
                "/user-service/api/v1/reinitialize-pwd",
                "/user-service/api/v1/update-pwd",
                "/referentiel-service/api/v1/licence"));

        laissePasser = new AtomicBoolean(false);
        suite = mock(GatewayFilterChain.class);
        when(suite.filter(any())).thenAnswer(invocation -> {
            laissePasser.set(true);
            return Mono.empty();
        });
    }

    private void licence(boolean actionsOuvertes, String... modules) {
        when(cache.etat()).thenReturn(Mono.just(
                new EtatLicenceCache.Etat(actionsOuvertes, List.of(modules), "abonnement terminé")));
    }

    private MockServerWebExchange requete(HttpMethod methode, String chemin) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.method(methode, chemin).build());
    }

    private int statut(MockServerWebExchange echange) {
        return echange.getResponse().getStatusCode() == null
                ? 200 : echange.getResponse().getStatusCode().value();
    }

    // ------------------------------------------------------------ la consultation reste ouverte

    @Test
    @DisplayName("Une lecture passe même sans licence valide : les données ne sont pas prises en otage")
    void lecture_toujoursOuverte() {
        licence(false);
        MockServerWebExchange echange = requete(HttpMethod.GET,
                "/amelioration-service/api/v1/non-conformite/all");

        filtre.filter(echange, suite).block();

        assertThat(laissePasser).isTrue();
    }

    @Test
    @DisplayName("Le contrôle préalable du navigateur n'est pas une écriture")
    void optionsPasse() {
        licence(false);
        MockServerWebExchange echange = requete(HttpMethod.OPTIONS, "/support-service/api/v1/qms");

        filtre.filter(echange, suite).block();

        // Refusé, la requête réelle échouerait avec un message parlant de CORS — introuvable.
        assertThat(laissePasser).isTrue();
    }

    // ------------------------------------------------------------ licence expirée

    @Test
    @DisplayName("Une écriture est refusée en 402 quand la licence n'est plus valide")
    void ecriture_refuseeSansLicence() {
        licence(false);
        MockServerWebExchange echange = requete(HttpMethod.POST,
                "/amelioration-service/api/v1/non-conformite/create");

        filtre.filter(echange, suite).block();

        // 402 et non 403 : l'écran doit dire « votre abonnement a pris fin », et non « vous n'avez
        // pas le droit », qui enverrait réclamer des droits déjà détenus.
        assertThat(statut(echange)).isEqualTo(402);
        assertThat(laissePasser).isFalse();
    }

    @Test
    @DisplayName("Se connecter reste possible : sinon une licence expirée serait irrécupérable")
    void connexion_toujoursOuverte() {
        licence(false);
        MockServerWebExchange echange = requete(HttpMethod.POST, "/user-service/api/v1/login");

        filtre.filter(echange, suite).block();

        assertThat(laissePasser).isTrue();
    }

    @Test
    @DisplayName("Installer une licence reste possible quand la licence a expiré")
    void installation_toujoursOuverte() {
        licence(false);
        MockServerWebExchange echange = requete(HttpMethod.POST, "/referentiel-service/api/v1/licence");

        // Le blocage se retournerait autrement contre nous, un vendredi soir.
        filtre.filter(echange, suite).block();

        assertThat(laissePasser).isTrue();
    }

    @Test
    @DisplayName("Reprendre la main sur son mot de passe reste possible, licence échue")
    void motDePasse_toujoursOuvert() {
        licence(false);

        // La réinitialisation tient en deux temps. Exempter la demande sans la reprise laissait
        // l'utilisateur devant un lien de courriel qui n'aboutissait pas.
        for (String chemin : List.of("/user-service/api/v1/initiate-reset-pwd",
                "/user-service/api/v1/reinitialize-pwd",
                "/user-service/api/v1/update-pwd")) {
            laissePasser.set(false);
            filtre.filter(requete(HttpMethod.PUT, chemin), suite).block();
            assertThat(laissePasser).as(chemin).isTrue();
        }
    }

    @Test
    @DisplayName("Réinitialiser le mot de passe d'un tiers reste soumis à la licence")
    void motDePasseDUnTiers_soumisALaLicence() {
        licence(false);
        MockServerWebExchange echange = requete(HttpMethod.PATCH,
                "/user-service/api/v1/users/reset-password");

        // Administrer les comptes des autres n'est pas récupérer son propre accès : rien n'oblige
        // à l'ouvrir pour qu'une installation expirée reste réactivable.
        filtre.filter(echange, suite).block();

        assertThat(laissePasser).isFalse();
        assertThat(statut(echange)).isEqualTo(402);
    }

    // ------------------------------------------------------------ modules souscrits

    @Test
    @DisplayName("Une écriture sur un module souscrit passe")
    void moduleSouscrit_passe() {
        licence(true, "NON_CONFORMITE", "DOCUMENTAIRE");
        MockServerWebExchange echange = requete(HttpMethod.POST,
                "/amelioration-service/api/v1/non-conformite/create");

        filtre.filter(echange, suite).block();

        assertThat(laissePasser).isTrue();
    }

    @Test
    @DisplayName("Une écriture sur un module jamais acheté est refusée")
    void moduleNonSouscrit_refuse() {
        licence(true, "NON_CONFORMITE", "DOCUMENTAIRE");
        MockServerWebExchange echange = requete(HttpMethod.POST,
                "/amelioration-service/api/v1/reclamation/create");

        // C'est le trou d'origine : un curl authentifié créait une réclamation sur une plateforme
        // qui n'avait jamais souscrit le module.
        filtre.filter(echange, suite).block();

        assertThat(statut(echange)).isEqualTo(402);
        assertThat(laissePasser).isFalse();
    }

    @Test
    @DisplayName("Le préfixe le plus long l'emporte : réclamations et risques ne sont pas des NC")
    void prefixeLePlusLong() {
        // Les trois chemins vivent dans le même service, et relèvent de trois modules distincts.
        assertThat(filtre.moduleDe("/amelioration-service/api/v1/reclamation/create"))
                .isEqualTo("RECLAMATION");
        assertThat(filtre.moduleDe("/amelioration-service/api/v1/risque/create"))
                .isEqualTo("RISQUE");
        assertThat(filtre.moduleDe("/amelioration-service/api/v1/non-conformite/create"))
                .isEqualTo("NON_CONFORMITE");
    }

    @Test
    @DisplayName("Un chemin sans module déclaré n'exige que la validité de la licence")
    void cheminSansModule() {
        assertThat(filtre.moduleDe("/workflow-service/api/v1/workflows")).isNull();
        assertThat(filtre.moduleDe("/user-service/api/v1/users/create")).isNull();

        licence(true, "NON_CONFORMITE");
        MockServerWebExchange echange = requete(HttpMethod.POST, "/workflow-service/api/v1/workflows");

        // Le moteur de workflow sert tous les modules à la fois : lui exiger un module précis
        // fermerait les circuits de ceux qui sont pourtant souscrits.
        filtre.filter(echange, suite).block();

        assertThat(laissePasser).isTrue();
    }

    @Test
    @DisplayName("S'exécute après la résolution de l'utilisateur")
    void ordreApresAuthentification() {
        // AuthCookieFilter est à -1 : il a résolu l'utilisateur et ses permissions avant que ce
        // filtre ne décide.
        assertThat(filtre.getOrder()).isGreaterThan(new AuthCookieFilter(
                org.springframework.web.reactive.function.client.WebClient.builder(),
                new UserPermissionsCache(60)).getOrder());
    }
}
