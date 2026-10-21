package com.qualiapproche.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La limitation du rythme des appels d'authentification.
 *
 * <p>Ce qui compte : qu'un essai de mots de passe en série soit arrêté, qu'un utilisateur ne paie
 * pas pour un autre, et que l'adresse cliente ne se choisisse pas en écrivant un en-tête.</p>
 */
class FiltreLimiteDeDebitTest {

    private static final String CONNEXION = "/user-service/api/v1/login";

    private final AtomicLong maintenant = new AtomicLong(1_000_000_000L);
    private FiltreLimiteDeDebit filtre;
    private AtomicInteger passages;
    private WebFilterChain suite;

    @BeforeEach
    void setUp() {
        filtre = new FiltreLimiteDeDebit(maintenant::get);
        FiltreLimiteDeDebit.Regle connexion = new FiltreLimiteDeDebit.Regle();
        connexion.setChemins(List.of(CONNEXION));
        connexion.setCapacite(3);
        connexion.setPeriode(Duration.ofMinutes(1));
        Map<String, FiltreLimiteDeDebit.Regle> regles = new LinkedHashMap<>();
        regles.put("connexion", connexion);
        filtre.setRegles(regles);

        passages = new AtomicInteger();
        suite = echange -> {
            passages.incrementAndGet();
            return Mono.empty();
        };
    }

    private MockServerWebExchange appeler(HttpMethod methode, String chemin, String ip, String transmis) {
        MockServerHttpRequest.BaseBuilder<?> requete = MockServerHttpRequest.method(methode, chemin)
                .remoteAddress(new InetSocketAddress(ip, 40000));
        if (transmis != null) {
            requete.header("X-Forwarded-For", transmis);
        }
        MockServerWebExchange echange = MockServerWebExchange.from(requete.build());
        filtre.filter(echange, suite).block();
        return echange;
    }

    private MockServerWebExchange connexion(String ip) {
        return appeler(HttpMethod.POST, CONNEXION, ip, null);
    }

    @Test
    @DisplayName("Au-delà du seuil, la connexion reçoit 429 avec Retry-After")
    void auDelaDuSeuil_429() {
        for (int i = 0; i < 3; i++) {
            assertThat(connexion("10.0.0.1").getResponse().getStatusCode()).isNull();
        }

        MockServerWebExchange refusee = connexion("10.0.0.1");

        assertThat(passages).hasValue(3);
        assertThat(refusee.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        // Un jeton toutes les 20 s (3 par minute) : c'est l'attente annoncée.
        assertThat(refusee.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("20");
        assertThat(refusee.getResponse().getBodyAsString().block()).contains("\"motif\":\"LIMITE\"");
    }

    @Test
    @DisplayName("Le seau se remplit avec le temps : on peut réessayer après l'attente annoncée")
    void apresLAttente_denouveauAdmis() {
        for (int i = 0; i < 4; i++) {
            connexion("10.0.0.1");
        }
        maintenant.addAndGet(Duration.ofSeconds(20).toNanos());

        assertThat(connexion("10.0.0.1").getResponse().getStatusCode()).isNull();
        assertThat(passages).hasValue(4);
    }

    @Test
    @DisplayName("Chaque adresse a son propre seau : un utilisateur ne paie pas pour un autre")
    void seauParAdresse() {
        for (int i = 0; i < 4; i++) {
            connexion("10.0.0.1");
        }

        assertThat(connexion("10.0.0.2").getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Les chemins hors règle et les contrôles préalables ne sont pas comptés")
    void horsRegle_etOptions_nonComptes() {
        for (int i = 0; i < 10; i++) {
            appeler(HttpMethod.GET, "/amelioration-service/api/v1/nc", "10.0.0.1", null);
            appeler(HttpMethod.OPTIONS, CONNEXION, "10.0.0.1", null);
        }

        assertThat(connexion("10.0.0.1").getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Une variante d'écriture du chemin n'échappe pas à la règle")
    void varianteDeChemin_couverte() {
        for (int i = 0; i < 3; i++) {
            connexion("10.0.0.1");
        }

        assertThat(appeler(HttpMethod.POST, "/user-service//api/v1/LOGIN/", "10.0.0.1", null)
                .getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Derrière le mandataire, c'est l'adresse qu'il a vue qui compte, non celle que le client écrit")
    void adresseCliente_luePartantDeLaFin() {
        // Le mandataire (10.9.9.9 vu par la passerelle) ajoute en fin de liste l'adresse réelle ;
        // le client a écrit ce qu'il voulait au début.
        for (int i = 0; i < 3; i++) {
            appeler(HttpMethod.POST, CONNEXION, "10.9.9.9", "1.1.1." + i + ", 203.0.113.7");
        }

        MockServerWebExchange refusee = appeler(HttpMethod.POST, CONNEXION, "10.9.9.9", "8.8.8.8, 203.0.113.7");

        assertThat(refusee.getResponse().getStatusCode())
                .as("changer le début de X-Forwarded-For ne doit pas donner un nouveau seau")
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Sans X-Forwarded-For, ou s'il est ignoré, l'adresse de la connexion fait foi")
    void sansEnTete_adresseDistante() {
        MockServerHttpRequest requete = MockServerHttpRequest.post(CONNEXION)
                .remoteAddress(new InetSocketAddress("192.0.2.5", 40000)).build();
        assertThat(filtre.adresseCliente(requete)).isEqualTo("192.0.2.5");

        filtre.setSautsDeConfiance(0);
        MockServerHttpRequest avecEnTete = MockServerHttpRequest.post(CONNEXION)
                .remoteAddress(new InetSocketAddress("192.0.2.5", 40000))
                .header("X-Forwarded-For", "203.0.113.7").build();
        assertThat(filtre.adresseCliente(avecEnTete)).isEqualTo("192.0.2.5");
    }

    @Test
    @DisplayName("Désactivée, la limitation laisse tout passer")
    void desactivee_toutPasse() {
        filtre.setActif(false);
        for (int i = 0; i < 10; i++) {
            connexion("10.0.0.1");
        }

        assertThat(passages).hasValue(10);
    }

    @Test
    @DisplayName("Le seau admet une rafale, puis un jeton par intervalle")
    void seauDeJetons_rafalePuisRythme() {
        SeauDeJetons seau = new SeauDeJetons(2, Duration.ofSeconds(10), 0);

        assertThat(seau.prendre(0)).isZero();
        assertThat(seau.prendre(0)).isZero();
        assertThat(seau.prendre(0)).isEqualTo(5);
        assertThat(seau.prendre(Duration.ofSeconds(5).toNanos())).isZero();
        // Le remplissage ne dépasse jamais la capacité, même après une longue absence.
        long tard = Duration.ofHours(1).toNanos();
        assertThat(seau.prendre(tard)).isZero();
        assertThat(seau.prendre(tard)).isZero();
        assertThat(seau.prendre(tard)).isPositive();
    }
}
