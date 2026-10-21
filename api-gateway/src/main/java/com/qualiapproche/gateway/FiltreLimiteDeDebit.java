package com.qualiapproche.gateway;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Limite le rythme des appels d'authentification, par adresse cliente.
 *
 * <p><b>Pourquoi.</b> La connexion, le rafraîchissement de session et la réinitialisation du mot
 * de passe sont ouverts à tous, par nature. Sans limite, rien n'empêchait d'essayer des mots de
 * passe en série, ou de faire envoyer des milliers de courriels de réinitialisation à un
 * collaborateur — chaque appel aboutissant jusqu'à Keycloak et au serveur de messagerie.</p>
 *
 * <p><b>Comment.</b> Un {@link SeauDeJetons} par règle et par adresse, en mémoire. Pas de Redis :
 * la passerelle tourne en un seul exemplaire, et une limite approximative suffit à rendre
 * l'attaque inutile. Les seaux vivent dans un cache borné, qui oublie ceux qui n'ont pas servi
 * depuis la période : une rafale d'adresses inventées ne fait pas grossir la mémoire sans fin.
 * Le refus est un 429 avec {@code Retry-After}, que le frontal peut traduire en « réessayez dans
 * une minute ».</p>
 *
 * <p><b>L'adresse cliente.</b> Derrière le mandataire du déploiement, l'adresse vue par la
 * passerelle est la sienne — tous les utilisateurs partageraient un seul seau. L'adresse réelle
 * est dans {@code X-Forwarded-For}. Mais seule la partie ajoutée par nos propres mandataires est
 * digne de foi : le début de la liste est ce que le client a bien voulu écrire, et le prendre
 * permettrait de changer d'adresse à chaque essai. D'où {@code sauts-de-confiance} : le nombre de
 * mandataires à nous devant la passerelle, et donc l'entrée à lire en partant de la fin.</p>
 *
 * <p><b>Les seuils</b> sont réglables : une organisation entière sort souvent par une seule
 * adresse publique, et le seuil de connexion doit absorber l'arrivée du matin.</p>
 */
@Component
@ConfigurationProperties(prefix = "qualisira.limites")
@Slf4j
public class FiltreLimiteDeDebit implements WebFilter, Ordered {

    /** Une règle : les chemins qu'elle couvre et le nombre d'appels admis par période. */
    public static class Regle {
        private List<String> chemins = new ArrayList<>();
        private int capacite = 10;
        private Duration periode = Duration.ofMinutes(1);

        public List<String> getChemins() {
            return chemins;
        }

        public void setChemins(List<String> chemins) {
            this.chemins = chemins;
        }

        public int getCapacite() {
            return capacite;
        }

        public void setCapacite(int capacite) {
            this.capacite = capacite;
        }

        public Duration getPeriode() {
            return periode;
        }

        public void setPeriode(Duration periode) {
            this.periode = periode;
        }
    }

    /** Coupe tout : utile pour un test de charge, jamais en production. */
    private boolean actif = true;

    /** Mandataires à nous devant la passerelle (voir la Javadoc de la classe). 0 : ignorer l'en-tête. */
    private int sautsDeConfiance = 1;

    /** Nombre de seaux gardés en mémoire au plus, toutes règles confondues. */
    private long tailleMax = 100_000;

    private Map<String, Regle> regles = new LinkedHashMap<>();

    private final LongSupplier horloge;
    private volatile Cache<String, SeauDeJetons> seaux;

    public FiltreLimiteDeDebit() {
        this(System::nanoTime);
    }

    /** Pour les tests : une horloge qu'on avance à la main. */
    FiltreLimiteDeDebit(LongSupplier horloge) {
        this.horloge = horloge;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest requete = exchange.getRequest();
        // Le contrôle préalable d'un navigateur précède chaque appel : le compter diviserait le
        // seuil par deux.
        if (!actif || HttpMethod.OPTIONS.equals(requete.getMethod())) {
            return chain.filter(exchange);
        }

        String chemin = normaliser(requete.getURI().getPath());
        for (Map.Entry<String, Regle> entree : regles.entrySet()) {
            Regle regle = entree.getValue();
            if (!couvre(regle, chemin)) {
                continue;
            }
            String adresse = adresseCliente(requete);
            SeauDeJetons seau = seaux().get(entree.getKey() + "|" + adresse,
                    cle -> new SeauDeJetons(regle.getCapacite(), regle.getPeriode(), horloge.getAsLong()));
            long attente = seau.prendre(horloge.getAsLong());
            if (attente > 0) {
                log.warn("Trop d'appels « {} » depuis {} sur {} : refusé pour {} s.",
                        entree.getKey(), adresse, chemin, attente);
                exchange.getResponse().getHeaders().set("Retry-After", Long.toString(attente));
                return ReponseDeRefus.ecrire(exchange, HttpStatus.TOO_MANY_REQUESTS, "LIMITE",
                        "Trop de tentatives en peu de temps. Patientez " + attente
                                + " seconde(s) avant de réessayer.");
            }
            // Une seule règle par chemin : la première qui le couvre.
            break;
        }
        return chain.filter(exchange);
    }

    /**
     * Le chemin ramené à une forme unique : minuscules, barres obliques dédoublées, sans barre
     * finale. Sans cela, {@code //user-service/api/v1/login/} échapperait à la règle tout en
     * atteignant peut-être le même service.
     */
    static String normaliser(String chemin) {
        String c = chemin.toLowerCase(Locale.ROOT).replaceAll("/{2,}", "/");
        while (c.length() > 1 && c.endsWith("/")) {
            c = c.substring(0, c.length() - 1);
        }
        return c;
    }

    private static boolean couvre(Regle regle, String chemin) {
        return regle.getChemins().stream()
                .map(FiltreLimiteDeDebit::normaliser)
                .anyMatch(prefixe -> chemin.equals(prefixe) || chemin.startsWith(prefixe + "/"));
    }

    /** L'adresse du client, lue comme la Javadoc de la classe l'explique. */
    String adresseCliente(ServerHttpRequest requete) {
        String transmis = requete.getHeaders().getFirst("X-Forwarded-For");
        if (sautsDeConfiance > 0 && transmis != null && !transmis.isBlank()) {
            String[] sauts = transmis.split(",");
            // Chacun de nos mandataires ajoute en fin de liste l'adresse qu'il a vue : la n-ième
            // en partant de la fin est celle que le plus extérieur d'entre eux a vue.
            int indice = Math.max(0, sauts.length - sautsDeConfiance);
            String adresse = sauts[indice].trim();
            if (!adresse.isEmpty()) {
                return adresse;
            }
        }
        InetSocketAddress distante = requete.getRemoteAddress();
        if (distante == null) {
            return "inconnue";
        }
        return distante.getAddress() != null ? distante.getAddress().getHostAddress() : distante.getHostString();
    }

    /**
     * Le cache des seaux, créé au premier appel : les règles ne sont connues qu'une fois la
     * configuration liée.
     *
     * <p>Un seau oublié au bout d'une période d'inactivité serait de toute façon redevenu plein :
     * l'oublier ne change rien à la limite, et libère la mémoire.</p>
     */
    private Cache<String, SeauDeJetons> seaux() {
        Cache<String, SeauDeJetons> cache = seaux;
        if (cache == null) {
            synchronized (this) {
                cache = seaux;
                if (cache == null) {
                    Duration plusLongue = regles.values().stream()
                            .map(Regle::getPeriode)
                            .max(Duration::compareTo)
                            .orElse(Duration.ofMinutes(1));
                    cache = Caffeine.newBuilder()
                            .maximumSize(tailleMax)
                            .expireAfterAccess(plusLongue)
                            .build();
                    seaux = cache;
                }
            }
        }
        return cache;
    }

    @Override
    public int getOrder() {
        return RangsDesFiltres.LIMITE_DE_DEBIT;
    }

    // ---------------------------------------------------------------- configuration

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public int getSautsDeConfiance() {
        return sautsDeConfiance;
    }

    public void setSautsDeConfiance(int sautsDeConfiance) {
        this.sautsDeConfiance = sautsDeConfiance;
    }

    public long getTailleMax() {
        return tailleMax;
    }

    public void setTailleMax(long tailleMax) {
        this.tailleMax = tailleMax;
    }

    public Map<String, Regle> getRegles() {
        return regles;
    }

    public void setRegles(Map<String, Regle> regles) {
        this.regles = regles;
    }
}
