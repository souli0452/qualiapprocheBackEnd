package com.qualiapproche.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Origines autorisées à appeler la passerelle depuis un navigateur.
 *
 * <p>La liste était <b>écrite en dur</b> : {@code https://qualisira.horeb.tech} et
 * {@code http://localhost:4200}. Changer de domaine imposait donc de modifier ce fichier, de
 * reconstruire l'image et de la redéployer — et tant que ce n'était pas fait, le frontal recevait au
 * premier appel une erreur qui parle de CORS sans jamais nommer l'origine attendue. C'est ce qui
 * s'est produit au passage sur {@code test} : le frontal a changé de domaine, la passerelle ne le
 * connaissait pas, et la connexion échouait avant même d'atteindre le service.</p>
 *
 * <p>Deux réglages, parce qu'ils ne se valent pas :</p>
 * <ul>
 *   <li>{@code qualisira.cors.origines} — des origines <b>exactes</b>. C'est ce qu'on veut en
 *       production : on nomme le frontal, et rien d'autre n'entre.</li>
 *   <li>{@code qualisira.cors.motifs-origines} — des <b>motifs</b>, pour couvrir d'un coup un
 *       ensemble d'origines dont les noms bougent encore.</li>
 * </ul>
 *
 * <p>Les identifiants sont autorisés ({@code allowCredentials}) parce que l'authentification passe
 * par un cookie {@code access_token} et que le frontal appelle en {@code withCredentials}. Cela
 * <b>interdit</b> l'origine {@code *} : la norme refuse le joker dès que la requête porte des
 * identifiants, et le navigateur rejette la réponse sans autre explication. Le motif {@code *}, en
 * revanche, est licite — Spring renvoie alors l'origine réellement appelante à la place du joker,
 * ce que la norme accepte. C'est la seule façon d'ouvrir à toutes les origines sans casser
 * l'authentification.</p>
 *
 * <p><b>Ce que l'ouverture totale coûte</b>, puisque cela ne se voit pas à l'usage : n'importe quel
 * site visité par un utilisateur connecté peut appeler cette API avec ses cookies de session, et
 * donc agir en son nom. Rien d'autre ne s'y oppose ici — la protection CSRF est désactivée et le
 * cookie est {@code SameSite=None}, qui ne filtre rien. C'est une configuration d'environnement de
 * test ; le journal la signale à chaque démarrage pour qu'elle ne passe pas en production sans
 * qu'on l'ait décidé.</p>
 */
@Configuration
@Slf4j
public class CorsConfig {

    /** Le motif qui ouvre à toutes les origines. */
    private static final String TOUTES_ORIGINES = "*";

    /**
     * Origines exactes, séparées par des virgules. Vide par défaut : c'est le motif qui ouvre.
     */
    @Value("${qualisira.cors.origines:}")
    private String origines;

    /** Motifs d'origines, séparés par des virgules. {@code *} ouvre à toutes. */
    @Value("${qualisira.cors.motifs-origines:*}")
    private String motifsOrigines;

    @Bean
    public CorsWebFilter corsWebFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration());
        return new CorsWebFilter(source);
    }

    /** Visible pour les tests : c'est la lecture des deux réglages qui mérite d'être figée. */
    CorsConfiguration configuration() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> exactes = decouper(origines);
        List<String> motifs = decouper(motifsOrigines);

        if (exactes.isEmpty() && motifs.isEmpty()) {
            // Une passerelle sans origine autorisée refuse tout appel de navigateur. Mieux vaut le
            // dire au démarrage que le laisser découvrir écran par écran.
            log.error("Aucune origine CORS n'est autorisée : renseignez qualisira.cors.origines "
                    + "ou qualisira.cors.motifs-origines, sinon aucun frontal ne pourra appeler "
                    + "cette passerelle.");
        }

        if (!exactes.isEmpty()) {
            config.setAllowedOrigins(exactes);
        }
        if (!motifs.isEmpty()) {
            config.setAllowedOriginPatterns(motifs);
        }

        if (motifs.contains(TOUTES_ORIGINES)) {
            // Dit à chaque démarrage, et en WARN : une API ouverte à toutes les origines ne se voit
            // pas à l'usage — tout fonctionne, précisément parce que tout est admis. C'est le seul
            // endroit où cela puisse se remarquer avant qu'un incident ne l'apprenne.
            log.warn("CORS — toutes les origines sont autorisées, cookies de session compris : "
                    + "n'importe quel site peut appeler cette API au nom d'un utilisateur connecté. "
                    + "Acceptable en test ; avant la production, nommez le frontal dans "
                    + "qualisira.cors.origines et videz qualisira.cors.motifs-origines.");
        } else {
            log.info("CORS — origines autorisées : {} ; motifs : {}",
                    exactes.isEmpty() ? "aucune" : exactes,
                    motifs.isEmpty() ? "aucun" : motifs);
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        // L'authentification passe par un cookie : sans cela, le navigateur n'en envoie aucun et
        // toute requête arrive anonyme.
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        return config;
    }

    /**
     * Découpe une liste séparée par des virgules.
     *
     * <p>Les blancs sont retirés : ces valeurs se posent à la main dans une console de déploiement,
     * où une espace après une virgule ne se voit pas — et produirait une origine que rien ne
     * reconnaîtrait jamais.</p>
     */
    private static List<String> decouper(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valeur.split(","))
                .map(String::trim)
                .filter(origine -> !origine.isEmpty())
                .toList();
    }
}
