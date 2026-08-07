package com.qualiapproche.common.config;

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
 * Les beans de ce module doivent vivre où les services les cherchent.
 *
 * <p>Chaque service ne balaie de {@code common} qu'un seul paquet :
 * {@code com.qualiapproche.common.config} — c'est ce que déclare le {@code scanBasePackages} de
 * chacun. Un {@code @Component} posé ailleurs dans ce module n'est enregistré <b>nulle part</b> : il
 * compile, les tests unitaires qui le construisent à la main passent, et le service qui l'injecte
 * refuse de démarrer. C'est exactement ce qui est arrivé à l'envoi de courriels par gabarit.</p>
 *
 * <p>Ce projet ne démarre aucun contexte Spring en test : rien ne l'aurait signalé avant le
 * lancement. Ce test lit donc les sources et refuse toute annotation de stéréotype hors du paquet
 * balayé. Un bean partagé s'y déclare par une méthode {@code @Bean} dans une configuration, comme le
 * fait {@link CourrielConfig}.</p>
 */
class BeansPartagesVisiblesTest {

    /** Seul paquet de ce module que les services balaient. */
    private static final String PAQUET_BALAYE =
            Path.of("com", "qualiapproche", "common", "config").toString();

    /**
     * Stéréotypes cherchés, sous leur forme courte comme pleinement qualifiée.
     *
     * <p>Le projet écrit volontiers {@code @org.springframework.stereotype.Component} sans import :
     * ne chercher que la forme courte laisserait passer exactement ce que ce test doit voir.</p>
     */
    private static final Pattern STEREOTYPE = Pattern.compile(
            "^@(?:[A-Za-z0-9_]+\\.)*("
                    + "Component|Service|Repository|Controller|RestController"
                    + "|Configuration|ControllerAdvice|RestControllerAdvice)\\b");

    @Test
    @DisplayName("Aucun stéréotype Spring hors du paquet balayé par les services")
    void stereotypes_uniquementDansLePaquetBalaye() throws IOException {
        Path sources = Path.of("src", "main", "java");
        assertThat(sources).as("sources du module introuvables depuis %s", Path.of("").toAbsolutePath())
                .exists();

        List<String> horsPaquet = new ArrayList<>();
        try (Stream<Path> fichiers = Files.walk(sources)) {
            for (Path fichier : fichiers.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (fichier.toString().contains(PAQUET_BALAYE)) {
                    continue;
                }
                // Sur une ligne d'annotation, et non dans un commentaire ou un texte : la
                // documentation de ces classes cite volontiers le piège qu'elle explique.
                Files.readAllLines(fichier, StandardCharsets.UTF_8).stream()
                        .map(String::trim)
                        .filter(ligne -> STEREOTYPE.matcher(ligne).find())
                        .findFirst()
                        .ifPresent(ligne ->
                                horsPaquet.add(sources.relativize(fichier) + " porte " + ligne));
            }
        }

        assertThat(horsPaquet)
                .as("Ces classes ne seront chargées par aucun service : les services ne balaient de "
                        + "ce module que « com.qualiapproche.common.config ». Déclarez le bean par une "
                        + "méthode @Bean dans une configuration de ce paquet, comme CourrielConfig.")
                .isEmpty();
    }
}
