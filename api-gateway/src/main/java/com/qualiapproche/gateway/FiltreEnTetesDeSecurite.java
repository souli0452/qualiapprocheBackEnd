package com.qualiapproche.gateway;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

/**
 * Pose les en-têtes de sécurité sur toutes les réponses qui sortent de la passerelle.
 *
 * <p>La passerelle est la seule porte que voit un navigateur : c'est donc ici qu'ils se posent une
 * fois pour toutes, et non dans chacun des dix services — qui ne les posaient pas tous, et pas
 * sur leurs réponses d'erreur. Un scanner (ZAP) les réclame sur chaque réponse, refus compris.</p>
 *
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff} — un fichier déposé ne sera jamais interprété
 *       comme un script parce qu'il en a l'air ;</li>
 *   <li>{@code X-Frame-Options: DENY} et {@code frame-ancestors 'none'} — aucune réponse de
 *       l'API ne s'affiche dans un cadre : le frontal lit les documents par requête et les montre
 *       depuis un {@code blob:}, jamais en encadrant une adresse de la passerelle ;</li>
 *   <li>{@code Content-Security-Policy: default-src 'none'} — l'API ne sert que des données, pas
 *       de pages : rien n'a à s'y exécuter. L'interface Swagger, quand elle est activée, reçoit
 *       une politique relâchée juste assez pour s'afficher ;</li>
 *   <li>{@code Referrer-Policy: no-referrer} et une {@code Permissions-Policy} qui coupe caméra,
 *       micro, géolocalisation, paiement ;</li>
 *   <li>{@code Strict-Transport-Security} — seulement quand la requête est arrivée en HTTPS,
 *       directement ou derrière le mandataire du déploiement ({@code X-Forwarded-Proto}) : posé
 *       sur du HTTP, il ne signifierait rien, et sur le poste de développement il empoisonnerait
 *       {@code localhost} ;</li>
 *   <li>{@code Cache-Control: no-store} — imposé sur les réponses d'authentification, qui portent
 *       les cookies de session ; proposé ailleurs, si le service n'a rien dit.</li>
 * </ul>
 *
 * <p><b>Un en-tête déjà posé par le service aval est respecté</b> (sauf le {@code Cache-Control}
 * d'authentification) : un service sait mieux que la passerelle ce que vaut sa réponse — un
 * fichier téléchargé peut légitimement se mettre en cache, par exemple. Les en-têtes sont posés
 * au moment où la réponse part ({@code beforeCommit}), c'est-à-dire après que ceux du service y
 * ont été recopiés.</p>
 */
@Component
public class FiltreEnTetesDeSecurite implements WebFilter, Ordered {

    static final String POLITIQUE_API = "default-src 'none'; frame-ancestors 'none'";

    /**
     * Ce dont l'interface Swagger a besoin : ses propres scripts et feuilles de style, des images
     * en {@code data:}, et des appels vers la passerelle elle-même. Les styles en ligne sont ceux
     * que la bibliothèque injecte ; aucun script en ligne n'est admis.
     */
    static final String POLITIQUE_SWAGGER = "default-src 'self'; script-src 'self'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; "
            + "frame-ancestors 'none'";

    static final String PERMISSIONS = "camera=(), microphone=(), geolocation=(), payment=(), usb=()";

    /** Un an, sous-domaines compris : la durée que réclament les outils d'évaluation. */
    static final String HSTS = "max-age=31536000; includeSubDomains";

    /** Les chemins de l'interface Swagger et de la description qu'elle lit. */
    private static final List<String> CHEMINS_SWAGGER = List.of(
            "/swagger-ui", "/webjars/", "/v3/api-docs");

    /** Les réponses qui posent ou renouvellent les cookies de session. */
    private static final List<String> CHEMINS_AUTHENTIFICATION = List.of(
            "/user-service/api/v1/login",
            "/user-service/api/v1/logout",
            "/user-service/api/v1/refresh",
            "/user-service/api/v1/initiate-reset-pwd",
            "/user-service/api/v1/reinitialize-pwd",
            "/user-service/api/v1/update-pwd",
            "/user-service/api/v1/verify-email",
            "/user-service/api/v1/me");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest requete = exchange.getRequest();
        String chemin = requete.getURI().getPath();
        boolean https = estArriveeEnHttps(requete);

        exchange.getResponse().beforeCommit(() -> {
            poser(exchange.getResponse().getHeaders(), chemin, https);
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    /** Visible pour les tests : la décision, sans la mécanique réactive. */
    static void poser(HttpHeaders entetes, String chemin, boolean https) {
        siAbsent(entetes, "X-Content-Type-Options", "nosniff");
        siAbsent(entetes, "X-Frame-Options", "DENY");
        siAbsent(entetes, "Referrer-Policy", "no-referrer");
        siAbsent(entetes, "Permissions-Policy", PERMISSIONS);
        siAbsent(entetes, "Content-Security-Policy", estSwagger(chemin) ? POLITIQUE_SWAGGER : POLITIQUE_API);
        if (https) {
            siAbsent(entetes, "Strict-Transport-Security", HSTS);
        }
        if (estAuthentification(chemin)) {
            // Imposé, et non proposé : une réponse qui porte un cookie de session ne doit rester
            // dans aucun cache, quoi qu'ait dit le service.
            entetes.set(HttpHeaders.CACHE_CONTROL, "no-store");
            entetes.set(HttpHeaders.PRAGMA, "no-cache");
        } else {
            siAbsent(entetes, HttpHeaders.CACHE_CONTROL, "no-store");
        }
    }

    private static void siAbsent(HttpHeaders entetes, String nom, String valeur) {
        if (!entetes.containsKey(nom)) {
            entetes.set(nom, valeur);
        }
    }

    /**
     * HTTPS vu par le client : directement, ou terminé par le mandataire du déploiement, qui le
     * dit dans {@code X-Forwarded-Proto}. Un client qui mentirait sur cet en-tête n'obtiendrait
     * qu'un HSTS de plus pour lui-même.
     */
    static boolean estArriveeEnHttps(ServerHttpRequest requete) {
        if ("https".equalsIgnoreCase(requete.getURI().getScheme())) {
            return true;
        }
        String proto = requete.getHeaders().getFirst("X-Forwarded-Proto");
        if (proto == null) {
            return false;
        }
        // Plusieurs mandataires en chaîne ajoutent chacun le leur : le premier est celui du client.
        String premier = proto.split(",")[0].trim().toLowerCase(Locale.ROOT);
        return "https".equals(premier);
    }

    private static boolean estSwagger(String chemin) {
        return chemin.equals("/swagger-ui.html") || CHEMINS_SWAGGER.stream().anyMatch(chemin::startsWith);
    }

    private static boolean estAuthentification(String chemin) {
        return CHEMINS_AUTHENTIFICATION.stream()
                .anyMatch(prefixe -> chemin.equals(prefixe) || chemin.startsWith(prefixe + "/"));
    }

    @Override
    public int getOrder() {
        return RangsDesFiltres.EN_TETES_DE_SECURITE;
    }
}
