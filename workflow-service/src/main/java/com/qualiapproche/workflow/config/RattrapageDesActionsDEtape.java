package com.qualiapproche.workflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ouvre les étapes existantes à plus de deux actions.
 *
 * <p>Une étape n'en offrait que deux — approuver, rejeter — parce que l'action se confondait avec
 * sa décision : l'unicité en base portait sur le couple (étape, décision), et une troisième suite
 * était refusée par le schéma lui-même. C'est désormais le <b>code</b> de l'action qui l'identifie
 * dans son étape, la décision ne disant plus que sa nature.</p>
 *
 * <p>Deux gestes, qu'aucune mise à jour automatique du schéma ne ferait à notre place : Hibernate
 * en mode {@code update} ajoute des colonnes et des contraintes, il n'en retire jamais. L'ancienne
 * unicité serait donc restée en place sur toutes les bases en service, et l'administrateur qui
 * ajoute une action depuis l'éditeur aurait buté sur une violation de contrainte sans rapport
 * lisible avec ce qu'il faisait.</p>
 *
 * <p>Les transitions déjà enregistrées reçoivent le nom de leur décision comme code : c'est ce
 * qu'elles portaient de fait, et l'appariement lors d'une modification du circuit s'y retrouve.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(110) // avant RattrapageDesCircuitsLivres, qui ajoute des transitions
public class RattrapageDesActionsDEtape implements CommandLineRunner {

    private static final String ANCIENNE_UNICITE = "uk_workflow_transition_from_decision";

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        leverLAncienneUnicite();
        nommerLesActionsSansCode();
    }

    private void leverLAncienneUnicite() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE workflow_transition DROP CONSTRAINT IF EXISTS " + ANCIENNE_UNICITE);
            log.info("Unicité « {} » levée : une étape peut porter plusieurs actions de même nature.",
                    ANCIENNE_UNICITE);
        } catch (Exception e) {
            // Une base neuve ne l'a jamais eue, et le rattrapage ne doit pas empêcher le service de
            // démarrer : il rend possible une configuration nouvelle, il ne conditionne rien de ce
            // qui fonctionne déjà.
            log.warn("L'unicité « {} » n'a pas pu être levée : une étape restera limitée à une action "
                    + "par nature tant qu'elle subsiste. Cause : {}", ANCIENNE_UNICITE, e.getMessage());
        }
    }

    private void nommerLesActionsSansCode() {
        try {
            int nommees = jdbcTemplate.update(
                    "UPDATE workflow_transition SET code = decision WHERE code IS NULL OR code = ''");
            if (nommees > 0) {
                log.info("{} action(s) d'étape nommée(s) d'après leur décision.", nommees);
            }
        } catch (Exception e) {
            log.warn("Les actions sans code n'ont pas pu être nommées : {}", e.getMessage());
        }
    }
}
