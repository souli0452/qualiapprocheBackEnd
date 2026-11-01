package com.qualiapproche.common.cloisonnement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate ne filtre pas le SQL natif sur la direction : une requête native qui l'oublie montre à
 * un client les lignes de tous les autres, sans erreur ni avertissement. C'est le piège qui
 * reviendra le plus, à chaque nouvelle requête écrite à la main.
 *
 * <p>Ce test lit les sources de tous les modules et exige, autour de chaque requête native, soit un
 * filtre sur la direction, soit la mention « Cloisonnement : sans objet » qui dit pourquoi la
 * table n'appartient à aucun client.</p>
 */
class RequetesNativesCloisonneesTest {

    private static final Pattern NATIVE = Pattern.compile("nativeQuery\\s*=\\s*true|createNativeQuery\\(");
    private static final Pattern FILTRE = Pattern.compile(
            "direction_id|DE_LA_DIRECTION|directionId|:direction\\b|Cloisonnement : sans objet");
    /** Lignes lues avant et après la requête : sa Javadoc, son texte, ses paramètres. */
    private static final int AVANT = 14;
    private static final int APRES = 8;

    @Test
    @DisplayName("Toute requête native filtre la direction, ou dit pourquoi elle n'a pas à le faire")
    void requetesNatives_cloisonnees() throws IOException {
        Path depot = Path.of("..").toAbsolutePath().normalize();
        List<String> sansFiltre = new ArrayList<>();
        try (Stream<Path> fichiers = Files.walk(depot)) {
            for (Path fichier : fichiers
                    .filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> f.toString().contains("/src/main/java/"))
                    .filter(f -> !f.toString().contains("/.claude/") && !f.toString().contains("/target/"))
                    .toList()) {
                List<String> lignes = Files.readAllLines(fichier, StandardCharsets.UTF_8);
                for (int i = 0; i < lignes.size(); i++) {
                    if (!NATIVE.matcher(lignes.get(i)).find()) {
                        continue;
                    }
                    String voisinage = String.join("\n",
                            lignes.subList(Math.max(0, i - AVANT), Math.min(lignes.size(), i + APRES)));
                    if (!FILTRE.matcher(voisinage).find()) {
                        sansFiltre.add(depot.relativize(fichier) + ":" + (i + 1));
                    }
                }
            }
        }
        assertThat(sansFiltre)
                .as("Requêtes natives sans filtre de direction : ajoutez « direction_id = … » (voir "
                        + "NonConformiteRepository.DE_LA_DIRECTION), ou, pour une table qui n'est à aucun "
                        + "client, la mention « Cloisonnement : sans objet » et sa raison.")
                .isEmpty();
    }
}
