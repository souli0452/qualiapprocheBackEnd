package com.qualiapproche.amelioration.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Télécharge le logo de l'organisation pour l'incorporer à un document imprimé.
 *
 * <p>L'adresse vient d'un réglage ({@code LOGO_URL}) saisi depuis l'écran de paramétrage, et le
 * <b>serveur</b> la suit. Lue par {@code new URL(adresse).openConnection()}, elle acceptait
 * n'importe quel schéma et n'importe quelle destination : {@code file:///etc/passwd} faisait lire
 * un fichier du serveur, {@code http://169.254.169.254/…} interrogeait les métadonnées de l'hôte,
 * {@code http://localhost:8092/actuator/…} visait les services internes — et le contenu revenait,
 * encodé en base64, dans le PDF remis à l'appelant. Une falsification de requête côté serveur
 * (SSRF) doublée d'une lecture de fichiers.</p>
 *
 * <p>Désormais :</p>
 * <ul>
 *   <li>{@code https} seulement — {@code http} si et seulement si
 *       {@code amelioration.logo.http-admis} l'autorise, pour un réseau interne qui n'a pas de
 *       certificat ;</li>
 *   <li>l'hôte est résolu, et <b>toutes</b> ses adresses doivent être publiques : ni boucle
 *       locale, ni réseau privé, ni lien local (les métadonnées des hébergeurs y vivent), ni
 *       adresse indéterminée, ni multidiffusion ;</li>
 *   <li>aucune redirection n'est suivie : elle mènerait là où la vérification n'a pas regardé ;</li>
 *   <li>2 Mo au plus, et un type {@code image/*} déclaré par le serveur — un logo n'est ni une
 *       page HTML ni un SVG, que le moteur de rendu irait à son tour interpréter.</li>
 * </ul>
 *
 * <p>Tout refus a la même issue qu'un réglage vide : le document part sans logo. Un en-tête sans
 * image vaut mieux qu'aucun document, et la raison est journalisée pour l'administrateur.</p>
 *
 * <p><b>Limite connue :</b> l'hôte est résolu une fois pour la vérification et une seconde fois
 * par le client HTTP. Un serveur DNS hostile qui changerait sa réponse entre les deux (rebinding)
 * passerait la garde ; s'en prémunir exigerait de se connecter à l'adresse vérifiée en conservant
 * le nom pour TLS, ce que le client du JDK ne permet pas. Le filtrage sortant du réseau reste la
 * défense de fond.</p>
 */
@Component
@Slf4j
public class LogoDeLOrganisation {

    /** Au-delà, ce n'est plus un logo : la fiche ne doit pas embarquer un fichier quelconque. */
    public static final int TAILLE_MAX = 2 * 1024 * 1024;

    /** Délai au-delà duquel le logo est abandonné : le document ne doit pas attendre une image. */
    private static final Duration DELAI = Duration.ofSeconds(5);

    private final boolean httpAdmis;
    private final Resolveur resolveur;
    private final Predicate<InetAddress> destinationInterdite;
    private final HttpClient client;

    /** Résolution d'un nom d'hôte ; remplaçable dans les tests, qui n'ont pas de DNS à disposition. */
    @FunctionalInterface
    public interface Resolveur {
        InetAddress[] resoudre(String hote) throws UnknownHostException;
    }

    @Autowired
    public LogoDeLOrganisation(@Value("${amelioration.logo.http-admis:false}") boolean httpAdmis) {
        this(httpAdmis, InetAddress::getAllByName, LogoDeLOrganisation::adresseNonPublique);
    }

    /** Pour les tests : la résolution et la politique d'adresses sont fournies. */
    public LogoDeLOrganisation(boolean httpAdmis, Resolveur resolveur, Predicate<InetAddress> destinationInterdite) {
        this.httpAdmis = httpAdmis;
        this.resolveur = resolveur;
        this.destinationInterdite = destinationInterdite;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(DELAI)
                .build();
    }

    /**
     * Le logo en {@code data:} URI, prêt à incorporer ; {@code null} s'il est absent, refusé ou
     * illisible.
     */
    public String enDataUri(String adresse) {
        if (adresse == null || adresse.isBlank()) {
            return null;
        }
        URI uri = destinationAdmise(adresse.trim());
        if (uri == null) {
            return null;
        }
        try {
            HttpRequest requete = HttpRequest.newBuilder(uri).timeout(DELAI).GET().build();
            HttpResponse<InputStream> reponse = client.send(requete, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream flux = reponse.body()) {
                if (reponse.statusCode() != 200) {
                    log.warn("Logo de l'organisation non servi (HTTP {}, redirection comprise) : le document part sans lui.",
                            reponse.statusCode());
                    return null;
                }
                String type = typeImage(reponse.headers().firstValue("Content-Type").orElse(null));
                if (type == null) {
                    log.warn("Le logo de l'organisation n'est pas déclaré comme une image : le document part sans lui.");
                    return null;
                }
                long annonce = reponse.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (annonce > TAILLE_MAX) {
                    log.warn("Logo de l'organisation trop lourd ({} octets) : le document part sans lui.", annonce);
                    return null;
                }
                // La longueur annoncée n'engage pas le serveur : la lecture est bornée elle aussi.
                byte[] octets = flux.readNBytes(TAILLE_MAX + 1);
                if (octets.length == 0 || octets.length > TAILLE_MAX) {
                    log.warn("Logo de l'organisation vide ou au-delà de {} octets : le document part sans lui.", TAILLE_MAX);
                    return null;
                }
                return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(octets);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException | RuntimeException e) {
            log.warn("Logo de l'organisation illisible ({}) : le document part sans lui.", e.getMessage());
            return null;
        }
    }

    /**
     * L'adresse, si le serveur peut la suivre ; {@code null} sinon, raison journalisée.
     *
     * <p>Toutes les adresses de l'hôte sont examinées, et non la première : un nom qui en rend une
     * publique et une interne laisserait le client choisir.</p>
     */
    URI destinationAdmise(String adresse) {
        URI uri;
        try {
            uri = new URI(adresse);
        } catch (Exception e) {
            log.warn("Adresse du logo illisible : le document part sans lui.");
            return null;
        }
        String schema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(schema) && !("http".equals(schema) && httpAdmis)) {
            log.warn("Adresse du logo refusée : schéma « {} » non admis (https seulement{}).", schema,
                    httpAdmis ? ", ou http" : "");
            return null;
        }
        String hote = uri.getHost();
        if (hote == null || hote.isBlank() || uri.getUserInfo() != null) {
            log.warn("Adresse du logo refusée : hôte absent, ou identifiants dans l'adresse.");
            return null;
        }
        try {
            InetAddress[] adresses = resolveur.resoudre(hote);
            if (adresses == null || adresses.length == 0) {
                return null;
            }
            for (InetAddress ip : adresses) {
                if (destinationInterdite.test(ip)) {
                    log.warn("Adresse du logo refusée : {} désigne une destination interne ({}).", hote, ip.getHostAddress());
                    return null;
                }
            }
        } catch (UnknownHostException e) {
            log.warn("Adresse du logo refusée : hôte {} inconnu.", hote);
            return null;
        }
        return uri;
    }

    /**
     * Une adresse que le serveur n'a pas à joindre pour le compte d'un réglage.
     *
     * <p>Les adresses IPv6 qui embarquent une IPv4 (« ::ffff:127.0.0.1 ») sont ramenées à
     * celle-ci : sans quoi la boucle locale passait sous un autre nom. Les adresses uniques
     * locales IPv6 (fc00::/7), que {@code isSiteLocalAddress} ne reconnaît pas, sont ajoutées.</p>
     */
    public static boolean adresseNonPublique(InetAddress ip) {
        InetAddress examinee = ipv4Embarquee(ip);
        if (examinee.isLoopbackAddress() || examinee.isSiteLocalAddress() || examinee.isLinkLocalAddress()
                || examinee.isAnyLocalAddress() || examinee.isMulticastAddress()) {
            return true;
        }
        byte[] octets = examinee.getAddress();
        if (examinee instanceof Inet6Address) {
            return (octets[0] & 0xFE) == 0xFC;
        }
        // 100.64.0.0/10 : l'espace partagé des opérateurs, interne lui aussi.
        return (octets[0] & 0xFF) == 100 && (octets[1] & 0xC0) == 64;
    }

    private static InetAddress ipv4Embarquee(InetAddress ip) {
        if (!(ip instanceof Inet6Address)) {
            return ip;
        }
        byte[] o = ip.getAddress();
        boolean prefixeNul = true;
        for (int i = 0; i < 10; i++) {
            prefixeNul &= o[i] == 0;
        }
        boolean mappee = prefixeNul && (o[10] & 0xFF) == 0xFF && (o[11] & 0xFF) == 0xFF;
        boolean compatible = prefixeNul && o[10] == 0 && o[11] == 0
                && !((o[12] | o[13] | o[14]) == 0 && (o[15] == 0 || o[15] == 1));
        if (!mappee && !compatible) {
            return ip;
        }
        try {
            return InetAddress.getByAddress(new byte[]{o[12], o[13], o[14], o[15]});
        } catch (UnknownHostException e) {
            return ip;
        }
    }

    /** Le type déclaré, s'il est celui d'une image que le PDF peut porter telle quelle ; sinon {@code null}. */
    private static String typeImage(String contentType) {
        if (contentType == null) {
            return null;
        }
        String type = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!type.startsWith("image/") || type.contains("svg")) {
            return null;
        }
        return type;
    }
}
