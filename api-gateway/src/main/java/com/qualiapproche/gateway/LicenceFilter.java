package com.qualiapproche.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fait respecter la licence, à l'entrée de la plateforme.
 *
 * <p>C'est ici, et non dans les écrans, que se décide ce qui est permis. Le dispositif précédent
 * ne vivait que dans le navigateur : un appel HTTP direct sur une ressource d'un module jamais
 * acheté aboutissait, licence expirée depuis deux ans comprise. Une licence qu'aucun serveur ne
 * vérifie n'est pas une licence.</p>
 *
 * <p>Deux règles, et une seule exception :</p>
 * <ul>
 *   <li><b>la consultation ne se ferme jamais</b> — les lectures passent toujours. Couper l'accès
 *       aux données qualité d'un client transformerait un retard de paiement en litige, et le
 *       pousserait à chercher comment contourner ;</li>
 *   <li><b>les écritures exigent une licence valide</b>, et que le module visé soit souscrit ;</li>
 *   <li>l'installation d'une licence et l'authentification restent ouvertes : sans cela, une
 *       licence expirée empêcherait de se connecter pour en poser une nouvelle — le blocage se
 *       retournerait contre nous, un vendredi soir.</li>
 * </ul>
 *
 * <p>Le refus est un <b>402</b>, distinct du 403 d'une permission manquante : l'écran doit pouvoir
 * dire « votre abonnement a pris fin » et non « vous n'avez pas le droit », qui enverrait
 * l'utilisateur réclamer des droits qu'il détient déjà.</p>
 *
 * <p>Ordre 0 : après {@link AuthCookieFilter} (-1), qui a résolu l'utilisateur.</p>
 */
@Component
@ConfigurationProperties(prefix = "gateway.licence")
@Slf4j
public class LicenceFilter implements GlobalFilter, Ordered {

    /**
     * Chemins soustraits au contrôle, même en écriture.
     *
     * <p>Se connecter, se déconnecter, rafraîchir sa session, installer une licence ou démarrer
     * un essai : tout ce sans quoi une installation expirée ne pourrait plus jamais être
     * réactivée.</p>
     */
    private List<String> exemptions = List.of(
            "/user-service/api/v1/login",
            "/user-service/api/v1/logout",
            "/user-service/api/v1/refresh",
            "/user-service/api/v1/initiate-reset-pwd",
            "/referentiel-service/api/v1/licence");

    /**
     * Module exigé selon le début du chemin, du plus précis au plus général.
     *
     * <p>Déclaré en configuration et non dans le code : le découpage des services évolue, et une
     * correspondance erronée fermerait une fonctionnalité pourtant payée — cela doit se corriger
     * sans livrer une version.</p>
     */
    private Map<String, String> modules = new LinkedHashMap<>();

    private final EtatLicenceCache licence;

    public LicenceFilter(EtatLicenceCache licence) {
        this.licence = licence;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String chemin = exchange.getRequest().getURI().getPath();
        HttpMethod methode = exchange.getRequest().getMethod();

        if (estUneLecture(methode) || estExempte(chemin)) {
            return chain.filter(exchange);
        }

        return licence.etat().flatMap(etat -> {
            if (!etat.actionsOuvertes()) {
                return refuser(exchange, etat.message() != null ? etat.message()
                        : "La licence de cette installation n'est plus valide : les actions sont "
                          + "suspendues. Vos données restent consultables.");
            }

            String moduleExige = moduleDe(chemin);
            if (moduleExige != null && !etat.ouvre(moduleExige)) {
                return refuser(exchange, "Le module « " + moduleExige + " » ne fait pas partie de "
                        + "votre abonnement. Contactez l'éditeur pour l'ajouter.");
            }

            return chain.filter(exchange);
        });
    }

    /**
     * {@code OPTIONS} compris : le contrôle préalable d'un navigateur n'est pas une écriture, et
     * le refuser ferait échouer la requête réelle avec un message qui parlerait de CORS.
     */
    private boolean estUneLecture(HttpMethod methode) {
        return HttpMethod.GET.equals(methode)
                || HttpMethod.HEAD.equals(methode)
                || HttpMethod.OPTIONS.equals(methode);
    }

    private boolean estExempte(String chemin) {
        return exemptions.stream().anyMatch(chemin::startsWith);
    }

    /**
     * Le module dont relève ce chemin, ou {@code null} s'il n'en relève d'aucun.
     *
     * <p>Le préfixe le plus long l'emporte : les réclamations et les risques vivent dans le même
     * service que les non-conformités, et relèvent pourtant de modules distincts.</p>
     */
    String moduleDe(String chemin) {
        return modules.entrySet().stream()
                .filter(entree -> chemin.startsWith(entree.getKey()))
                .max(Comparator.comparingInt(entree -> entree.getKey().length()))
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    /**
     * 402 « Payment Required » — le seul code qui dise la vraie raison.
     *
     * <p>Le message est rédigé pour être affiché tel quel : c'est la seule information dont
     * dispose l'utilisateur pour comprendre qu'il ne s'agit ni d'une panne, ni d'un défaut de
     * droits.</p>
     */
    private Mono<Void> refuser(ServerWebExchange exchange, String message) {
        log.info("Action refusée faute de licence : {} {}",
                exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath());

        ServerHttpResponse reponse = exchange.getResponse();
        reponse.setStatusCode(HttpStatus.PAYMENT_REQUIRED);
        reponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String corps = "{\"message\":\"" + message.replace("\"", "'") + "\","
                + "\"motif\":\"LICENCE\"}";
        DataBuffer tampon = reponse.bufferFactory().wrap(corps.getBytes(StandardCharsets.UTF_8));
        return reponse.writeWith(Mono.just(tampon));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    // ---------------------------------------------------------------- configuration

    public void setExemptions(List<String> exemptions) {
        this.exemptions = exemptions;
    }

    public void setModules(Map<String, String> modules) {
        this.modules = modules;
    }

    public Map<String, String> getModules() {
        return modules;
    }

    public List<String> getExemptions() {
        return exemptions;
    }
}
