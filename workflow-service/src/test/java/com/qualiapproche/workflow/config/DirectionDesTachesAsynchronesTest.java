package com.qualiapproche.workflow.config;

import com.qualiapproche.common.cloisonnement.Cloisonnement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskDecorator;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le courriel d'étape et la ligne de la boîte de réception partent d'un fil asynchrone, sans jeton :
 * ils doivent s'y exécuter sous la direction du dossier, sans quoi ils liraient les gabarits de
 * tous les clients.
 */
class DirectionDesTachesAsynchronesTest {

    private final TaskDecorator decorateur = new WorkflowAppConfig().directionDesTachesAsynchrones();

    @Test
    @DisplayName("La direction présente à la soumission est rétablie dans le fil de la tâche")
    void directionReportee() throws Exception {
        UUID direction = UUID.randomUUID();
        AtomicReference<UUID> vue = new AtomicReference<>();

        Runnable tache = Cloisonnement.pour(direction,
                () -> decorateur.decorate(() -> vue.set(Cloisonnement.directionDuClient())));
        Thread fil = new Thread(tache);
        fil.start();
        fil.join();

        assertThat(vue.get()).isEqualTo(direction);
    }

    @Test
    @DisplayName("Soumise hors de tout client, la tâche reste telle quelle")
    void sansDirection_tacheIntacte() {
        Runnable tache = () -> { };
        assertThat(decorateur.decorate(tache)).isSameAs(tache);
    }
}
