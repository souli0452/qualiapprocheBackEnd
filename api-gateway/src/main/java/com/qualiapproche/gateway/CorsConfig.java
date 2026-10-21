package com.qualiapproche.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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
 * <p><b>Le défaut n'est plus le joker.</b> Le motif {@code *} était livré par défaut : n'importe
 * quel site visité par un utilisateur connecté pouvait appeler cette API avec ses cookies de
 * session, et donc agir en son nom — la protection CSRF est désactivée dans les services et le
 * cookie est {@code SameSite=None}, qui ne filtre rien. Une évaluation de sécurité le relève au
 * premier passage. Le défaut nomme désormais les domaines où le produit est réellement servi
 * ({@link #MOTIFS_PAR_DEFAUT}). La variable d'environnement {@code QUALISIRA_CORS_MOTIFS_ORIGINES}
 * <b>remplace</b> cette liste, elle ne s'y ajoute pas : pour déclarer un frontal de plus, on la
 * renseigne avec la liste par défaut recopiée <i>et</i> le nouvel hôte — sans quoi les frontaux
 * existants et le poste de développement perdraient l'accès. Le joker reste possible pour un
 * environnement de test — signalé à chaque démarrage.</p>
 *
 * <p>La configuration n'est lue qu'une fois et partagée : le filtre anti-CSRF
 * ({@link FiltreAntiCsrf}) juge les origines avec <b>la même</b> liste. Deux listes finiraient par
 * diverger, et l'écart se paierait soit d'un frontal refusé, soit d'une porte restée ouverte.</p>
 */
@Configuration
@Slf4j
public class CorsConfig {

    /** Le motif qui ouvre à toutes les origines. */
    private static final String TOUTES_ORIGINES = "*";

    /**
     * Les frontaux où le produit est servi, quand rien d'autre n'est configuré.
     *
     * <p>Des <b>hôtes nommés</b>, et non des domaines entiers : {@code app.test.qualisira.com}, le
     * frontal de l'environnement de test, qui appelle {@code api-gateway.test.qualisira.com}. C'est
     * l'hôte qui sert réellement l'application Angular : on l'a vérifié sur le déploiement, et non
     * déduit des noms. Ni {@code qualisira.test.qualisira.com} ni l'ancien
     * {@code qualisira.horeb.tech} — cités par des tests et des exemples — n'ont d'enregistrement DNS,
     * et le domaine nu {@code qualisira.com} sert le site vitrine, pas l'application : aucun des
     * trois n'a à écrire dans l'API. Le poste de développement appelle depuis {@code localhost}, sur
     * le port que choisit {@code ng serve} : là seulement le motif est large, et il ne couvre que la
     * machine elle-même.</p>
     *
     * <p>Un motif comme {@code https://*.horeb.tech} a d'abord été retenu, puis écarté : il rend
     * fiable <b>tout hôte</b> du domaine, et pas seulement le frontal. Or {@code horeb.tech} sert
     * aussi le stockage objet ({@code s3.horeb.tech}, la console MinIO, les liens présignés) et
     * d'autres applications de la société. Une page HTML déposée dans un compartiment, ou une
     * faille XSS sur un site voisin, suffirait alors : les cookies {@code SameSite=None} partent, CORS
     * renvoie l'origine, l'anti-CSRF la laisse passer — et cette page lit les données de
     * l'utilisateur connecté et écrit en son nom. Le même raisonnement écarte
     * {@code *.qualisira.com}, qui couvrirait aussi la messagerie ou tout autre service du domaine.
     * Un nouveau frontal se déclare donc par la variable d'environnement, en le nommant — et en
     * recopiant ces motifs, qu'elle remplace.</p>
     */
    static final String MOTIFS_PAR_DEFAUT = "https://app.test.qualisira.com,"
            + "http://localhost:[*],http://127.0.0.1:[*]";

    /**
     * Origines exactes, séparées par des virgules. Vide par défaut : c'est le motif qui ouvre.
     */
    @Value("${qualisira.cors.origines:}")
    private String origines;

    /**
     * Motifs d'origines, séparés par des virgules. {@code *} ouvre à toutes. Une valeur configurée
     * remplace {@link #MOTIFS_PAR_DEFAUT} en entier.
     */
    @Value("${qualisira.cors.motifs-origines:" + MOTIFS_PAR_DEFAUT + "}")
    private String motifsOrigines;

    /**
     * La configuration, lue une seule fois : le filtre CORS et le filtre anti-CSRF la partagent.
     */
    @Bean
    public CorsConfiguration configurationCors() {
        return configuration();
    }

    /**
     * Le filtre CORS, placé juste après le filtre anti-CSRF.
     *
     * <p>Son rang est fixé, et non laissé au dernier par défaut : la limitation de débit
     * ({@link FiltreLimiteDeDebit}) vient après lui, pour que son 429 porte les en-têtes CORS. Sans
     * eux, le navigateur masque la réponse et le frontal croirait à une panne réseau au lieu
     * d'afficher « réessayez dans une minute ».</p>
     */
    @Bean
    public CorsWebFilter corsWebFilter(CorsConfiguration configurationCors) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configurationCors);
        return new FiltreCorsOrdonne(source);
    }

    /** Le {@link CorsWebFilter} de Spring, avec un rang. */
    static class FiltreCorsOrdonne extends CorsWebFilter implements Ordered {

        FiltreCorsOrdonne(UrlBasedCorsConfigurationSource source) {
            super(source);
        }

        @Override
        public int getOrder() {
            return RangsDesFiltres.CORS;
        }
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
                    + "Acceptable en test ; avant la production, retirez « * » de "
                    + "qualisira.cors.motifs-origines (QUALISIRA_CORS_MOTIFS_ORIGINES).");
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
