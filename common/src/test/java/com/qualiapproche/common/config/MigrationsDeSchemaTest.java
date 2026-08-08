package com.qualiapproche.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règles des migrations de schéma, vérifiées sur tous les services.
 *
 * <p>Flyway se tait sur trois erreurs qui coûtent cher, et qu'aucun test fonctionnel ne verrait :</p>
 * <ul>
 *   <li>un fichier mal nommé — {@code V2_ajout.sql} avec un seul souligné — est <b>ignoré</b> : la
 *       migration ne s'applique jamais, et le schéma diverge sans un mot ;</li>
 *   <li>deux scripts au même numéro de version font échouer le démarrage du service ;</li>
 *   <li>une directive propre à {@code psql} ({@code \\restrict}, {@code \\connect}) fait échouer la
 *       migration — le piège classique d'un script né d'un {@code pg_dump} recopié tel quel.</li>
 * </ul>
 *
 * <p>Ce test lit l'arborescence du dépôt depuis le module {@code common}, à l'image de
 * {@link BeansPartagesVisiblesTest} : la règle vaut pour tous les services, elle est donc écrite une
 * fois. Un service sans dossier de migration est simplement ignoré — les quatre services encore
 * vides n'ont ni entité ni base de données.</p>
 */
class MigrationsDeSchemaTest {

    private static final Pattern NOM_ATTENDU = Pattern.compile("^V(\\d+)__[\\w-]+\\.sql$");

    /** Le module {@code common} vit un niveau sous la racine du dépôt. */
    private static final Path RACINE = Path.of("..");

    private List<Path> dossiersDeMigration() throws IOException {
        try (Stream<Path> services = Files.list(RACINE)) {
            return services
                    .map(service -> service.resolve("src/main/resources/db/migration"))
                    .filter(Files::isDirectory)
                    .sorted()
                    .toList();
        }
    }

    /** Nom du service portant ce dossier de migration, pour que l'échec dise où chercher. */
    private String service(Path dossier) {
        return RACINE.relativize(dossier).getName(0).toString();
    }

    private List<Path> scripts(Path dossier) throws IOException {
        try (Stream<Path> fichiers = Files.list(dossier)) {
            return fichiers.filter(Files::isRegularFile).sorted().toList();
        }
    }

    @Test
    @DisplayName("Chaque service migré porte au moins son schéma initial")
    void servicesMigres_ontUnSchemaInitial() throws IOException {
        List<Path> dossiers = dossiersDeMigration();

        assertThat(dossiers)
                .as("aucun dossier de migration trouvé depuis %s : le test ne vérifie plus rien",
                        RACINE.toAbsolutePath().normalize())
                .isNotEmpty();

        for (Path dossier : dossiers) {
            assertThat(scripts(dossier))
                    .as("%s ne contient aucun script", dossier)
                    .anySatisfy(script -> assertThat(script.getFileName().toString()).startsWith("V1__"));
        }
    }

    @Test
    @DisplayName("Tout script suit le nommage V<n>__description.sql, faute de quoi Flyway l'ignore")
    void scripts_bienNommes() throws IOException {
        List<String> fautifs = new ArrayList<>();
        for (Path dossier : dossiersDeMigration()) {
            for (Path script : scripts(dossier)) {
                String nom = script.getFileName().toString();
                if (!NOM_ATTENDU.matcher(nom).matches()) {
                    fautifs.add(service(dossier) + " → " + nom);
                }
            }
        }

        assertThat(fautifs)
                .as("Ces fichiers ne seront pas appliqués — Flyway attend deux soulignés et une "
                        + "version numérique : V<n>__description.sql")
                .isEmpty();
    }

    @Test
    @DisplayName("Deux scripts ne partagent jamais la même version dans un service")
    void versions_uniquesParService() throws IOException {
        for (Path dossier : dossiersDeMigration()) {
            Map<String, String> parVersion = new LinkedHashMap<>();
            List<String> doublons = new ArrayList<>();
            for (Path script : scripts(dossier)) {
                Matcher m = NOM_ATTENDU.matcher(script.getFileName().toString());
                if (!m.matches()) {
                    continue;
                }
                String precedent = parVersion.put(m.group(1), script.getFileName().toString());
                if (precedent != null) {
                    doublons.add(precedent + " et " + script.getFileName());
                }
            }
            assertThat(doublons)
                    .as("%s : deux scripts de même version font échouer le démarrage du service",
                            dossier)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("Aucun script ne contient de directive propre à psql")
    void scripts_sansDirectivePsql() throws IOException {
        List<String> fautifs = new ArrayList<>();
        for (Path dossier : dossiersDeMigration()) {
            for (Path script : scripts(dossier)) {
                List<String> lignes = Files.readAllLines(script, StandardCharsets.UTF_8);
                for (int i = 0; i < lignes.size(); i++) {
                    String ligne = lignes.get(i).stripLeading();
                    // « \restrict », « \connect », « \i » : pg_dump en produit, Flyway ne les
                    // comprend pas et la migration échoue au premier démarrage.
                    if (ligne.startsWith("\\")) {
                        fautifs.add(script.getFileName() + ":" + (i + 1) + " → " + ligne.strip());
                    }
                }
            }
        }

        assertThat(fautifs)
                .as("Flyway exécute du SQL, pas des commandes psql : ces lignes feraient échouer "
                        + "la migration")
                .isEmpty();
    }
}
