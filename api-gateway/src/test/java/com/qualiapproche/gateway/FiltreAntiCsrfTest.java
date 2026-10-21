package com.qualiapproche.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le refus des écritures venues d'un site étranger.
 *
 * <p>Le cookie de session est {@code SameSite=None} et les services n'ont pas de protection CSRF :
 * sans ce filtre, une page piégée faisait agir le navigateur d'un utilisateur connecté en son nom.
 * Deux erreurs seraient graves en sens inverse : laisser passer une origine étrangère, et refuser
 * le frontal ou les appels entre services — qui casserait l'application entière.</p>
 */
class FiltreAntiCsrfTest {

    private FiltreAntiCsrf filtre;
    private AtomicBoolean laissePasser;
    private WebFilterChain suite;

    @BeforeEach
    void setUp() {
        CorsConfig cors = new CorsConfig();
        ReflectionTestUtils.setField(cors, "origines", "");
        ReflectionTestUtils.setField(cors, "motifsOrigines", CorsConfig.MOTIFS_PAR_DEFAUT);
        filtre = new FiltreAntiCsrf(cors.configuration());

        laissePasser = new AtomicBoolean(false);
        suite = echange -> {
            laissePasser.set(true);
            return Mono.empty();
        };
    }

    private MockServerWebExchange executer(MockServerHttpRequest.BaseBuilder<?> requete) {
        MockServerWebExchange echange = MockServerWebExchange.from(requete.build());
        filtre.filter(echange, suite).block();
        return echange;
    }

    private static MockServerHttpRequest.BodyBuilder ecriture(String chemin) {
        return MockServerHttpRequest.post("http://api-gateway.test.qualisira.com" + chemin);
    }

    @Test
    @DisplayName("Une écriture depuis un site étranger est refusée en 403, avec un motif lisible")
    void origineEtrangere_refusee() {
        MockServerWebExchange echange = executer(ecriture("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.ORIGIN, "https://site-piege.fr"));

        assertThat(laissePasser).isFalse();
        assertThat(echange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(echange.getResponse().getBodyAsString().block()).contains("\"motif\":\"ORIGINE\"");
    }

    @Test
    @DisplayName("Toutes les méthodes d'écriture sont couvertes")
    void toutesLesEcritures_couvertes() {
        for (HttpMethod methode : new HttpMethod[]{HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
            laissePasser.set(false);
            MockServerWebExchange echange = executer(MockServerHttpRequest
                    .method(methode, "http://api-gateway.test.qualisira.com/support-service/api/v1/document/1")
                    .header(HttpHeaders.ORIGIN, "https://site-piege.fr"));

            assertThat(laissePasser).as(methode.name()).isFalse();
            assertThat(echange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("Le frontal déployé et le poste de développement passent")
    void origineConnue_acceptee() {
        executer(ecriture("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.ORIGIN, "https://app.test.qualisira.com"));
        assertThat(laissePasser).isTrue();

        laissePasser.set(false);
        executer(ecriture("/user-service/api/v1/login").header(HttpHeaders.ORIGIN, "http://localhost:4200"));
        assertThat(laissePasser).isTrue();
    }

    @Test
    @DisplayName("Sans Origin ni Referer — appel entre services, script — la requête passe")
    void sansOrigine_acceptee() {
        MockServerWebExchange echange = executer(ecriture("/workflow-service/api/v1/instances"));

        assertThat(laissePasser).isTrue();
        assertThat(echange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("À défaut d'Origin, l'origine du Referer est jugée")
    void referer_juge() {
        executer(ecriture("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.REFERER, "https://site-piege.fr/page?x=1"));
        assertThat(laissePasser).isFalse();

        executer(ecriture("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.REFERER, "https://app.test.qualisira.com/non-conformite/suivi"));
        assertThat(laissePasser).isTrue();
    }

    @Test
    @DisplayName("Un Referer illisible est refusé : un navigateur n'en produit pas")
    void refererIllisible_refuse() {
        MockServerWebExchange echange = executer(ecriture("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.REFERER, "pas une adresse"));

        assertThat(laissePasser).isFalse();
        assertThat(echange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("L'origine « null » (cadre isolé, fichier local) est refusée")
    void origineNull_refusee() {
        executer(ecriture("/amelioration-service/api/v1/nc").header(HttpHeaders.ORIGIN, "null"));

        assertThat(laissePasser).isFalse();
    }

    @Test
    @DisplayName("Une lecture n'est jamais refusée, quelle qu'en soit l'origine")
    void lecture_jamaisRefusee() {
        // Une lecture ne modifie rien ; c'est CORS, et non ce filtre, qui empêche un site tiers
        // d'en lire la réponse.
        executer(MockServerHttpRequest.get("http://api-gateway.test.qualisira.com/amelioration-service/api/v1/nc")
                .header(HttpHeaders.ORIGIN, "https://site-piege.fr"));

        assertThat(laissePasser).isTrue();
    }

    @Test
    @DisplayName("Une requête de la passerelle vers elle-même (même origine) passe")
    void memeOrigine_acceptee() {
        // L'interface Swagger, quand elle est activée sur un hôte que la liste ne nomme pas.
        executer(MockServerHttpRequest.post("http://passerelle.interne:8088/user-service/api/v1/login")
                .header(HttpHeaders.HOST, "passerelle.interne:8088")
                .header(HttpHeaders.ORIGIN, "http://passerelle.interne:8088"));

        assertThat(laissePasser).isTrue();
    }

    @Test
    @DisplayName("Un sous-domaine imité ne trompe pas le filtre")
    void sousDomaineImite_refuse() {
        executer(ecriture("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.ORIGIN, "https://qualisira.com.attaquant.fr"));

        assertThat(laissePasser).isFalse();
    }

    @Test
    @DisplayName("Une page servie par le stockage objet (s3.horeb.tech) ne peut pas écrire")
    void stockageObjet_refuse() {
        // Même domaine que l'ancien frontal, mais un hôte qui sert du contenu déposé : le faire
        // passer reviendrait à laisser n'importe quel fichier HTML agir au nom de l'utilisateur.
        executer(ecriture("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.ORIGIN, "https://s3.horeb.tech"));

        assertThat(laissePasser).isFalse();
    }

    @Test
    @DisplayName("Les autres hôtes de test.qualisira.com ne peuvent pas écrire")
    void autresHotesDeTest_refuses() {
        // Seul app.test.qualisira.com sert le frontal : le domaine de test lui-même, ou un hôte
        // voisin, n'a pas à agir au nom de l'utilisateur.
        for (String origine : new String[]{"https://test.qualisira.com",
                "https://qualisira.test.qualisira.com", "https://api-gateway.test.qualisira.com"}) {
            laissePasser.set(false);
            executer(ecriture("/amelioration-service/api/v1/nc").header(HttpHeaders.ORIGIN, origine));
            assertThat(laissePasser).as(origine).isFalse();
        }
    }
}
