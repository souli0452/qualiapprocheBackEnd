package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.FieldType;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
import com.qualiapproche.workflow.model.WorkflowTransition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le circuit du plan d'action livré : qui traite, qui juge, et ce que devient un plan décliné.
 *
 * <p>Le plan d'action se distingue de la non-conformité sur un point : il connaît son responsable
 * dès sa naissance, là où la non-conformité doit d'abord être imputée. Le traitement n'a donc
 * aucune raison d'être ouvert au rôle « agent » — il l'était, ce qui laissait n'importe quel agent
 * solder le plan d'un autre, et par là ouvrir la clôture d'une non-conformité qui ne le concernait
 * pas.</p>
 *
 * <p>Le second piège est le plan décliné : sans issue, il reste indéfiniment à l'écart, et comme la
 * non-conformité n'est close que lorsque <b>tous</b> ses plans sont soldés, un seul refus suffirait
 * à bloquer le dossier pour toujours.</p>
 */
class CircuitPlanActionTest {

    /** Rôles réels de la plateforme, plus l'habilitation qui réserve une étape à son titulaire. */
    private static final Set<String> HABILITATIONS_ADMISES =
            Set.of("AGENT", "PILOTE", "RESPONSABLE_QUALITE", WorkflowDataInitializer.HABILITATION_TITULAIRE);

    private final Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();

    private WorkflowStep etape(String code) {
        return circuit.getSteps().stream()
                .filter(step -> code.equals(step.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Étape absente du circuit : " + code));
    }

    @Test
    @DisplayName("Aucune étape n'emploie une habilitation qui n'existe pas")
    void habilitations_toutesReelles() {
        assertThat(circuit.getSteps())
                .filteredOn(step -> step.getResponsableRole() != null)
                .extracting(WorkflowStep::getResponsableRole)
                .withFailMessage("Une habilitation inconnue ne désigne personne : l'étape serait "
                        + "indécidable, sans que rien ne le signale.")
                .allMatch(HABILITATIONS_ADMISES::contains);
    }

    @Test
    @DisplayName("La réalisation revient au responsable de l'action, non au rôle « agent »")
    void traitement_reserveAuTitulaire() {
        WorkflowStep aRealiser = etape("NON_TRAITER");

        assertThat(aRealiser.getResponsableRole())
                .withFailMessage("Ouverte au rôle AGENT, la réalisation d'une action serait décidable "
                        + "par tout agent, sur toute action.")
                .isEqualTo(WorkflowDataInitializer.HABILITATION_TITULAIRE);

        assertThat(aRealiser.getTransitions())
                .isNotEmpty()
                .allSatisfy(transition -> assertThat(transition.getRequiredRole())
                        .withFailMessage("Une transition sans habilitation propre rouvre à tous ce "
                                + "que l'étape avait réservé au titulaire.")
                        .isEqualTo(WorkflowDataInitializer.HABILITATION_TITULAIRE));
    }

    @Test
    @DisplayName("Le responsable peut décliner une action qui ne le concerne pas")
    void declinaison_possible() {
        assertThat(etape("NON_TRAITER").getTransitions())
                .extracting(WorkflowTransition::getDecision)
                .contains(StepDecision.REJETE);
    }

    @Test
    @DisplayName("Chaque temps de l'action a son responsable, et ce n'est jamais le même")
    void troisResponsabilitesDistinctes() {
        // Celui qui fait ne constate pas, celui qui constate ne juge pas de l'effet. Une action
        // dont le responsable déclare seul qu'elle est faite n'est pas une action vérifiée.
        assertThat(etape("NON_TRAITER").getResponsableRole())
                .isEqualTo(WorkflowDataInitializer.HABILITATION_TITULAIRE);
        assertThat(etape("EN_VERIFICATION").getResponsableRole()).isEqualTo("PILOTE");
        assertThat(etape("EFFICACITE_A_MESURER").getResponsableRole()).isEqualTo("RESPONSABLE_QUALITE");
        assertThat(etape("TRAITER").getResponsableRole()).isEqualTo("RESPONSABLE_QUALITE");
        assertThat(etape("REJECTED").getResponsableRole()).isEqualTo("PILOTE");
    }

    @Test
    @DisplayName("Toute transition porte l'habilitation de son étape")
    void transitions_toutesHabilitees() {
        // Une transition sans habilitation propre rouvre à tous ce que l'étape avait réservé :
        // l'étape dit qui voit le dossier, la transition dit qui décide.
        for (WorkflowStep etape : circuit.getSteps()) {
            assertThat(etape.getTransitions())
                    .allSatisfy(transition -> assertThat(transition.getRequiredRole())
                            .withFailMessage("La décision %s de l'étape « %s » n'exige aucune habilitation.",
                                    transition.getDecision(), etape.getCode())
                            .isEqualTo(etape.getResponsableRole()));
        }
    }

    @Test
    @DisplayName("L'action n'est soldée qu'après mesure de son efficacité")
    void soldeApresMesure() {
        // C'est le statut TRAITER qui autorise la clôture de la non-conformité. L'atteindre dès que
        // le responsable déclare l'action faite revenait à clore un dossier sans qu'aucun effet
        // n'ait été constaté.
        assertThat(etape("NON_TRAITER").getTransitions())
                .filteredOn(t -> t.getDecision() == StepDecision.APPROUVE)
                .allSatisfy(t -> assertThat(t.getToStep().getCode()).isEqualTo("EN_VERIFICATION"));
        assertThat(etape("EFFICACITE_A_MESURER").getTransitions())
                .filteredOn(t -> t.getDecision() == StepDecision.APPROUVE)
                .allSatisfy(t -> assertThat(t.getToStep().getCode()).isEqualTo("TRAITER"));
    }

    @Test
    @DisplayName("Une action jugée insuffisante repart chez son responsable")
    void rejets_renvoientALaRealisation() {
        for (String code : new String[] {"EN_VERIFICATION", "EFFICACITE_A_MESURER"}) {
            assertThat(etape(code).getTransitions())
                    .filteredOn(t -> t.getDecision() == StepDecision.REJETE)
                    .withFailMessage("L'étape « %s » ne permet pas de renvoyer l'action.", code)
                    .isNotEmpty()
                    .allSatisfy(t -> assertThat(t.getToStep().getCode())
                            .withFailMessage("Une action inefficace n'est pas une action close : elle "
                                    + "doit repartir chez celui qui en répond.")
                            .isEqualTo("NON_TRAITER"));
        }
    }

    @Test
    @DisplayName("Déclarer une action réalisée, c'est en rendre compte")
    void realisation_recueilleLeCompteRendu() {
        // Le compte rendu était saisi sur un écran et enregistré par un appel distinct de la
        // décision : celle-ci partant la première, l'action quittait l'étape de réalisation et ce
        // que le responsable avait écrit n'était plus accepté nulle part. Le pilote constatait alors
        // une action dont il ne lisait ni cause ni solution.
        WorkflowStep aRealiser = etape("NON_TRAITER");

        assertThat(aRealiser.getFields())
                .extracting(WorkflowStepField::getFieldName)
                .contains("causeIdentifiees", "solutionRetenues");

        assertThat(aRealiser.getFields())
                .filteredOn(f -> Set.of("causeIdentifiees", "solutionRetenues").contains(f.getFieldName()))
                .allSatisfy(champ -> {
                    assertThat(champ.isRequired())
                            .withFailMessage("Une action déclarée réalisée sans compte rendu ne peut "
                                    + "être ni vérifiée ni jugée efficace.")
                            .isTrue();
                    // Décliner une attribution n'oblige à rendre compte de rien : on n'a rien fait.
                    assertThat(champ.getDecision()).isEqualTo(StepDecision.APPROUVE);
                });
    }

    @Test
    @DisplayName("Le constat d'efficacité est exigé pour solder, et pour cela seulement")
    void constatEfficacite_exigePourSolder() {
        WorkflowStepField constat = etape("EFFICACITE_A_MESURER").getFields().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("L'étape de mesure ne recueille rien : "
                        + "elle ne serait qu'un passage de plus."));

        assertThat(constat.isRequired()).isTrue();
        // Exigé au renvoi aussi, il aurait demandé de constater une efficacité qu'on conteste.
        assertThat(constat.getDecision()).isEqualTo(StepDecision.APPROUVE);
    }

    @Test
    @DisplayName("Le circuit ne s'arrête qu'une fois l'action soldée")
    void seuleLaFinEstSansIssue() {
        for (WorkflowStep etape : circuit.getSteps()) {
            if ("TRAITER".equals(etape.getCode())) {
                assertThat(etape.getTransitions()).isEmpty();
            } else {
                assertThat(etape.getTransitions())
                        .withFailMessage("L'étape « %s » n'a aucune issue : l'action y resterait "
                                + "bloquée, et la non-conformité ne pourrait jamais être close.",
                                etape.getCode())
                        .isNotEmpty();
            }
        }
    }

    @Test
    @DisplayName("Un plan décliné revient dans le circuit au lieu d'y rester bloqué")
    void planDecline_nEstPasUneImpasse() {
        WorkflowStep rejete = etape("REJECTED");

        assertThat(rejete.getTransitions())
                .withFailMessage("Sans issue, un plan décliné bloque à jamais la clôture de sa "
                        + "non-conformité, qui exige que tous ses plans soient soldés.")
                .isNotEmpty();
        assertThat(rejete.getTransitions())
                .extracting(t -> t.getToStep().getCode())
                .contains("NON_TRAITER");
    }

    @Test
    @DisplayName("Ré-attribuer nomme un nouveau responsable, sinon le plan repart vers qui l'a décliné")
    void reattribution_nommeQuelquUn() {
        WorkflowStep rejete = etape("REJECTED");

        assertThat(rejete.getChampTitulaire())
                .withFailMessage("Sans champ titulaire, la ré-attribution renverrait le plan à "
                        + "celui-là même qui vient de le décliner : il tournerait en rond.")
                .isNotNull();

        WorkflowStepField champ = rejete.getFields().stream()
                .filter(f -> f.getFieldName().equals(rejete.getChampTitulaire()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Le champ titulaire déclaré n'est pas saisissable."));

        // Liste des collègues du pilote, et non saisie d'un identifiant technique que personne ne
        // connaît par cœur.
        assertThat(champ.getType()).isEqualTo(FieldType.SELECT);
        assertThat(champ.getOptions()).isNotBlank();
        assertThat(champ.isRequired()).isTrue();
    }

    @Test
    @DisplayName("La désignation n'est demandée que lorsqu'on ré-attribue")
    void champ_borneALaDecision() {
        WorkflowStep rejete = etape("REJECTED");

        assertThat(rejete.getFields())
                .filteredOn(f -> f.getFieldName().equals(rejete.getChampTitulaire()))
                .allSatisfy(f -> assertThat(f.getDecision())
                        .withFailMessage("Un champ hors portée s'afficherait aussi sur les autres "
                                + "décisions de l'étape, et y serait exigé sans raison.")
                        .isEqualTo(StepDecision.APPROUVE));
    }

    @Test
    @DisplayName("Aucune transition ne mène deux fois au même endroit pour la même décision")
    void aucunDoublonDeTransition() {
        for (WorkflowStep etape : circuit.getSteps()) {
            assertThat(etape.getTransitions())
                    .extracting(t -> t.getDecision() + " → " + t.getToStep().getCode())
                    .withFailMessage("Deux transitions identiques donnent deux boutons pour le même "
                            + "geste à l'étape %s.", etape.getCode())
                    .doesNotHaveDuplicates();
        }
    }
}
