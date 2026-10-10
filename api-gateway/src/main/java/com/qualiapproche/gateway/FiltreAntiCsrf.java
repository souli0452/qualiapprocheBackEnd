package com.qualiapproche.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Refuse les écritures venues d'un site étranger : la protection contre la falsification de
 * requête inter-sites (CSRF).
 *
 * <p><b>Pourquoi ici.</b> L'authentification passe par un cookie {@code access_token} posé en
 * {@code SameSite=None} — le frontal et la passerelle ne sont pas sur le même domaine, et le
 * navigateur ne l'enverrait pas autrement. Mais un tel cookie part aussi avec un formulaire
 * qu'un site quelconque fait soumettre en silence au navigateur d'un utilisateur connecté. Les
 * services ont désactivé la protection CSRF de Spring Security (elle suppose un jeton que le
 * frontal ne gère pas) : sans ce filtre, rien ne distinguait l'écran de l'application d'une page
 * piégée. Le filtre CORS ne suffit pas : un formulaire HTML ne déclenche aucun contrôle préalable,
 * et la requête aboutit même si le navigateur en cache ensuite la réponse.</p>
 *
 * <p><b>Comment.</b> Le navigateur déclare, sans que la page puisse le falsifier, l'origine d'une
 * écriture dans l'en-tête {@code Origin} — ou, pour de vieux navigateurs, dans {@code Referer}.
 * Une écriture ({@code POST}, {@code PUT}, {@code PATCH}, {@code DELETE}) dont l'origine déclarée
 * n'est pas admise par la <b>configuration CORS</b> est refusée. C'est la même liste, partagée et
 * non recopiée : deux listes finiraient par diverger.</p>
 *
 * <p><b>Ce qui passe sans origine.</b> Une requête qui ne déclare ni {@code Origin} ni
 * {@code Referer} n'est pas émise par une page web : c'est un appel entre services, un script
 * d'exploitation, un outil en ligne de commande. Ils n'ont pas de cookie de victime à détourner ;
 * les refuser casserait l'exploitation sans rien protéger.</p>
 *
 * <p>Une origine identique à celle de la passerelle elle-même passe aussi : c'est une requête
 * même-origine (l'interface Swagger quand elle est activée, par exemple), qu'aucun site tiers ne
 * peut émettre.</p>
 */
@Component
@Slf4j
public class FiltreAntiCsrf implements WebFilter, Ordered {

    /** Les méthodes qui modifient : les seules qu'une attaque CSRF ait intérêt à forger. */
    private static final Set<HttpMethod> ECRITURES =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

    private final CorsConfiguration configurationCors;

    public FiltreAntiCsrf(CorsConfiguration configurationCors) {
        this.configurationCors = configurationCors;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest requete = exchange.getRequest();
        if (!ECRITURES.contains(requete.getMethod())) {
            return chain.filter(exchange);
        }

        String origine = origineDeclaree(requete.getHeaders());
        if (origine == null || estAdmise(origine, requete)) {
            return chain.filter(exchange);
        }

        // L'origine est journalisée : c'est elle qu'il faudra ajouter à la configuration si un
        // frontal légitime vient de changer de domaine — l'erreur se diagnostique alors en lisant
        // une ligne, et non en devinant.
        log.warn("Écriture refusée, origine non autorisée : {} {} depuis « {} ». Si c'est un frontal "
                        + "légitime, ajoutez-le à QUALISIRA_CORS_MOTIFS_ORIGINES.",
                requete.getMethod(), requete.getURI().getPath(), origine);
        return ReponseDeRefus.ecrire(exchange, HttpStatus.FORBIDDEN, "ORIGINE",
                "Cette action a été émise depuis un site qui n'est pas autorisé à agir sur "
                        + "QualiSira. Reprenez-la depuis l'application.");
    }

    /**
     * L'origine que le navigateur déclare : {@code Origin}, à défaut celle du {@code Referer}.
     *
     * @return {@code null} si la requête n'en déclare aucune ; une chaîne vide si le
     *         {@code Referer} est illisible — ce qui sera refusé, un navigateur n'en produisant
     *         pas.
     */
    static String origineDeclaree(HttpHeaders entetes) {
        String origine = entetes.getOrigin();
        if (origine != null && !origine.isBlank()) {
            return origine.trim();
        }
        String referent = entetes.getFirst(HttpHeaders.REFERER);
        if (referent == null || referent.isBlank()) {
            return null;
        }
        return origineDe(referent.trim());
    }

    /** {@code schéma://hôte[:port]} d'une adresse, ou une chaîne vide si elle est illisible. */
    private static String origineDe(String adresse) {
        try {
            URI uri = URI.create(adresse);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                    + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        } catch (IllegalArgumentException illisible) {
            return "";
        }
    }

    private boolean estAdmise(String origine, ServerHttpRequest requete) {
        if (origine.isEmpty()) {
            return false;
        }
        return configurationCors.checkOrigin(origine) != null || estLaPasserelleElleMeme(origine, requete);
    }

    /**
     * Vrai si l'origine est celle de la passerelle : une requête même-origine.
     *
     * <p>L'hôte comparé est celui de l'en-tête {@code Host}, qu'une page tierce ne peut pas choisir
     * pour le navigateur de sa victime.</p>
     */
    private static boolean estLaPasserelleElleMeme(String origine, ServerHttpRequest requete) {
        String hote = requete.getHeaders().getFirst(HttpHeaders.HOST);
        if (hote == null || hote.isBlank()) {
            return false;
        }
        String origineLue = origineDe(origine);
        if (origineLue.isEmpty()) {
            return false;
        }
        String sansSchema = origineLue.substring(origineLue.indexOf("://") + 3);
        return sansSchema.equalsIgnoreCase(hote.trim());
    }

    @Override
    public int getOrder() {
        return RangsDesFiltres.ANTI_CSRF;
    }
}
