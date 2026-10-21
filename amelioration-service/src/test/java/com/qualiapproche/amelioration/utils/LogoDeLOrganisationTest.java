package com.qualiapproche.amelioration.utils;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le logo de l'organisation est une adresse que le serveur suit pour le compte d'un réglage.
 *
 * <p>Suivie sans garde, elle faisait lire au serveur ses propres fichiers ({@code file://}),
 * interroger les métadonnées de l'hébergeur ({@code 169.254.169.254}) ou viser les services
 * internes, et le contenu revenait dans le PDF. Ces tests fixent ce qui est refusé — et que le
 * refus a la même issue qu'un réglage vide : le document part sans logo.</p>
 */
class LogoDeLOrganisationTest {

    private HttpServer serveur;

    @AfterEach
    void arreter() {
        if (serveur != null) {
            serveur.stop(0);
        }
    }

    /** La politique réelle, avec une résolution fixée : le test n'a pas de DNS à disposition. */
    private LogoDeLOrganisation avecResolution(boolean httpAdmis, String... ips) {
        return new LogoDeLOrganisation(httpAdmis, hote -> {
            InetAddress[] adresses = new InetAddress[ips.length];
            for (int i = 0; i < ips.length; i++) {
                adresses[i] = InetAddress.getByName(ips[i]);
            }
            return adresses;
        }, LogoDeLOrganisation::adresseNonPublique);
    }

    // ------------------------------------------------------------------ refus

    @Test
    @DisplayName("file:// est refusé : le serveur ne lit pas ses propres fichiers pour un réglage")
    void schemaFichier_refuse() throws Exception {
        Path fichier = Files.createTempFile("secret", ".png");
        Files.writeString(fichier, "contenu-confidentiel");

        String logo = new LogoDeLOrganisation(true).enDataUri(fichier.toUri().toString());

        assertThat(logo).isNull();
    }

    @Test
    @DisplayName("La boucle locale est refusée, par adresse comme par nom")
    void boucleLocale_refusee() {
        assertThat(avecResolution(true, "127.0.0.1").destinationAdmise("https://127.0.0.1/logo.png")).isNull();
        assertThat(avecResolution(true, "127.0.0.1").destinationAdmise("https://localhost:8092/actuator/env")).isNull();
        assertThat(avecResolution(true, "::1").destinationAdmise("https://[::1]/logo.png")).isNull();
    }

    @Test
    @DisplayName("L'adresse des métadonnées de l'hébergeur (169.254.169.254) est refusée")
    void metadonneesHebergeur_refusees() {
        assertThat(avecResolution(true, "169.254.169.254")
                .destinationAdmise("http://169.254.169.254/latest/meta-data/")).isNull();
    }

    @Test
    @DisplayName("Réseaux privés, adresse indéterminée et multidiffusion sont refusés")
    void reseauxInternes_refuses() {
        for (String ip : new String[]{"10.0.0.5", "172.16.3.4", "192.168.1.10", "0.0.0.0", "224.0.0.1",
                "100.64.1.1", "fd00::1", "fe80::1"}) {
            assertThat(avecResolution(true, ip).destinationAdmise("https://logo.exemple.org/l.png"))
                    .as(ip).isNull();
        }
    }

    @Test
    @DisplayName("Un nom qui rend une adresse publique et une interne est refusé")
    void resolutionMixte_refusee() {
        assertThat(avecResolution(true, "93.184.216.34", "10.0.0.5")
                .destinationAdmise("https://logo.exemple.org/l.png")).isNull();
    }

    @Test
    @DisplayName("http est refusé tant qu'une propriété ne l'autorise pas")
    void http_refuseParDefaut() {
        assertThat(avecResolution(false, "93.184.216.34").destinationAdmise("http://logo.exemple.org/l.png")).isNull();
        assertThat(avecResolution(true, "93.184.216.34").destinationAdmise("http://logo.exemple.org/l.png")).isNotNull();
    }

    @Test
    @DisplayName("https vers une adresse publique est admis")
    void httpsPublic_admis() {
        assertThat(avecResolution(false, "93.184.216.34").destinationAdmise("https://logo.exemple.org/l.png"))
                .isNotNull();
    }

    @Test
    @DisplayName("Réglage vide : pas de logo, et pas d'erreur")
    void reglageVide_sansLogo() {
        assertThat(new LogoDeLOrganisation(false).enDataUri(null)).isNull();
        assertThat(new LogoDeLOrganisation(false).enDataUri("  ")).isNull();
    }

    // ------------------------------------------------------------------ téléchargement

    /**
     * Serveur local, joint en http avec une politique d'adresses neutre : seul le téléchargement
     * est jugé ici, la destination l'est par les tests précédents.
     */
    private LogoDeLOrganisation versServeurLocal() {
        return new LogoDeLOrganisation(true, InetAddress::getAllByName, ip -> false);
    }

    private String demarrer(String chemin, int statut, String type, byte[] corps, String redirection) throws Exception {
        serveur = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        serveur.createContext(chemin, echange -> {
            if (type != null) {
                echange.getResponseHeaders().add("Content-Type", type);
            }
            if (redirection != null) {
                echange.getResponseHeaders().add("Location", redirection);
            }
            echange.sendResponseHeaders(statut, corps.length == 0 ? -1 : corps.length);
            try (OutputStream sortie = echange.getResponseBody()) {
                sortie.write(corps);
            }
        });
        serveur.start();
        return "http://127.0.0.1:" + serveur.getAddress().getPort() + chemin;
    }

    @Test
    @DisplayName("Une image servie normalement est incorporée en data: URI")
    void image_incorporee() throws Exception {
        String adresse = demarrer("/logo.png", 200, "image/png", new byte[]{1, 2, 3}, null);

        assertThat(versServeurLocal().enDataUri(adresse)).isEqualTo("data:image/png;base64,AQID");
    }

    @Test
    @DisplayName("Une redirection n'est pas suivie : elle mènerait là où la garde n'a pas regardé")
    void redirection_nonSuivie() throws Exception {
        String adresse = demarrer("/logo.png", 302, null, new byte[0], "file:///etc/passwd");

        assertThat(versServeurLocal().enDataUri(adresse)).isNull();
    }

    @Test
    @DisplayName("Un contenu qui n'est pas une image est écarté, SVG compris")
    void typeNonImage_ecarte() throws Exception {
        String adresse = demarrer("/logo", 200, "text/html", "<html>".getBytes(StandardCharsets.UTF_8), null);
        assertThat(versServeurLocal().enDataUri(adresse)).isNull();
        arreter();

        String svg = demarrer("/logo.svg", 200, "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8), null);
        assertThat(versServeurLocal().enDataUri(svg)).isNull();
    }

    @Test
    @DisplayName("Au-delà de 2 Mo, le logo est écarté")
    void tropLourd_ecarte() throws Exception {
        String adresse = demarrer("/logo.png", 200, "image/png", new byte[LogoDeLOrganisation.TAILLE_MAX + 1], null);

        assertThat(versServeurLocal().enDataUri(adresse)).isNull();
    }
}
