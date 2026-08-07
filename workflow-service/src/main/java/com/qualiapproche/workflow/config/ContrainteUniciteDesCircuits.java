package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fait tenir par la base l'unicité du couple (famille, cible) des circuits ouvrables.
 *
 * <p>C'est ce couple qui rend le choix d'un circuit non ambigu : deux circuits réservés au même type
 * de document, ou deux circuits par défaut d'une même famille, laisseraient la décision à l'ordre
 * dans lequel la base rend ses lignes. Le service le refuse déjà à l'enregistrement ; la base le
 * refuse ici pour de bon, y compris à deux instances qui enregistreraient en même temps.</p>
 *
 * <p><b>Pourquoi en DDL explicite, et non par {@code @UniqueConstraint}.</b> Trois raisons, dont
 * chacune suffirait :</p>
 * <ul>
 *   <li>Hibernate journalise l'échec d'un {@code ALTER TABLE} et poursuit le démarrage : sur une
 *       base qui viole déjà la règle, la contrainte n'aurait jamais existé et l'on aurait cru
 *       protégé ce qui ne l'était pas ;</li>
 *   <li>en PostgreSQL, {@code UNIQUE} ne contraint pas les {@code NULL} : deux circuits par défaut
 *       — cible nulle — auraient passé la contrainte sans difficulté, alors que c'est le doublon le
 *       plus probable ;</li>
 *   <li>la règle ne porte que sur les circuits <b>ouvrables</b> : garder l'ancien circuit d'un type,
 *       désactivé, à côté du nouveau doit rester possible — il porte encore des dossiers en cours.
 *       Seul un index partiel l'exprime.</li>
 * </ul>
 *
 * <p>D'où deux index partiels, exactement calqués sur la règle du service : un pour les circuits
 * réservés, un pour le circuit par défaut de chaque famille.</p>
 *
 * <p><b>Sur une base qui viole déjà la règle, rien n'est créé.</b> Les couples en double sont
 * journalisés avec les noms et les dates de création des circuits en cause, et le démarrage se
 * poursuit : aucune correction automatique ne saurait deviner lequel de deux circuits concurrents
 * doit l'emporter, et refuser de démarrer priverait l'organisation de son outil pour une
 * configuration qui fonctionne — mal, mais elle fonctionne.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(130) // après les rattrapages, qui peuvent encore modifier les circuits
public class ContrainteUniciteDesCircuits implements CommandLineRunner {

    static final String INDEX_CIBLE_RESERVEE = "uk_workflow_famille_cible";
    static final String INDEX_CIRCUIT_PAR_DEFAUT = "uk_workflow_famille_defaut";

    private final JdbcTemplate jdbcTemplate;
    private final WorkflowRepository workflowRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, List<Workflow>> doublons = couplesEnDouble();
        if (!doublons.isEmpty()) {
            signaler(doublons);
            return;
        }

        creerLIndex(INDEX_CIBLE_RESERVEE,
                "CREATE UNIQUE INDEX IF NOT EXISTS " + INDEX_CIBLE_RESERVEE
                        + " ON workflow (resource_type, cible_id) WHERE cible_id IS NOT NULL AND actif");
        creerLIndex(INDEX_CIRCUIT_PAR_DEFAUT,
                "CREATE UNIQUE INDEX IF NOT EXISTS " + INDEX_CIRCUIT_PAR_DEFAUT
                        + " ON workflow (resource_type) WHERE cible_id IS NULL AND actif");

        verifier();
    }

    /**
     * Couples (famille, cible) portés par plus d'un circuit ouvrable.
     *
     * <p>Calculé en mémoire sur les circuits actifs — ils se comptent par dizaines — plutôt qu'en
     * SQL : la règle est la même que celle du service, et l'écrire deux fois en deux langages est le
     * meilleur moyen de les voir diverger.</p>
     */
    private Map<String, List<Workflow>> couplesEnDouble() {
        try {
            Map<String, List<Workflow>> parCouple = workflowRepository.findAll().stream()
                    .filter(Workflow::isActif)
                    .collect(Collectors.groupingBy(
                            circuit -> circuit.getResourceType() + " / "
                                    + (circuit.estLeCircuitParDefaut() ? "(par défaut)" : circuit.getCibleId()),
                            LinkedHashMap::new, Collectors.toList()));

            return parCouple.entrySet().stream()
                    .filter(entree -> entree.getValue().size() > 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (a, b) -> a, LinkedHashMap::new));
        } catch (Exception e) {
            // Base inaccessible ou schéma incomplet : on ne crée rien, et le service démarre.
            log.warn("Unicité des circuits non vérifiée : {}", e.getMessage());
            return Map.of();
        }
    }

    private void signaler(Map<String, List<Workflow>> doublons) {
        log.warn("Unicité des circuits NON posée en base : {} couple(s) (famille, cible) en double. "
                + "Le contrôle applicatif continue de refuser les nouveaux doublons, mais deux "
                + "instances simultanées pourraient encore en créer. Démêlez les circuits ci-dessous "
                + "— désactivez celui qui ne sert plus, ou réservez-le à une autre catégorie — puis "
                + "redémarrez : l'index sera posé de lui-même.", doublons.size());
        doublons.forEach((couple, circuits) -> log.warn("  {} → {}", couple,
                circuits.stream()
                        .map(circuit -> "« " + circuit.getNom() + " » (créé le " + circuit.getCreatedAt() + ")")
                        .collect(Collectors.joining(", "))));
    }

    private void creerLIndex(String nom, String ddl) {
        try {
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            // Un échec ici n'empêche rien de fonctionner : le contrôle applicatif reste en place.
            // La vérification qui suit dira que la base ne tient pas la règle.
            log.warn("L'index « {} » n'a pas pu être créé : {}", nom, e.getMessage());
        }
    }

    /**
     * Dit si la base tient effectivement la règle, au lieu de le supposer.
     *
     * <p>Sans cette vérification, un {@code CREATE INDEX} muet aurait laissé croire l'invariant
     * garanti — c'est exactement le piège qu'on cherche à éviter en ne confiant pas la contrainte à
     * Hibernate.</p>
     */
    private void verifier() {
        try {
            List<String> poses = jdbcTemplate.queryForList(
                    "SELECT indexname FROM pg_indexes WHERE tablename = 'workflow' AND indexname IN (?, ?)",
                    String.class, INDEX_CIBLE_RESERVEE, INDEX_CIRCUIT_PAR_DEFAUT);

            if (poses.size() == 2) {
                log.info("Unicité des circuits tenue par la base : un circuit par couple (famille, "
                        + "cible), un seul circuit par défaut par famille.");
            } else {
                log.warn("Unicité des circuits partiellement tenue par la base : index posé(s) {} sur "
                        + "les deux attendus. Le contrôle applicatif reste la seule garantie pour "
                        + "l'autre.", poses);
            }
        } catch (Exception e) {
            log.warn("Présence des index d'unicité non vérifiable : {}", e.getMessage());
        }
    }
}
