package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aligne les rôles inscrits sur les étapes avec les noms des rôles applicatifs.
 *
 * <p>Les circuits désignaient leurs responsables par {@code ROLE_AGENT}, {@code ROLE_PILOTE}…
 * alors que les rôles portent désormais les noms {@code AGENT}, {@code PILOTE}… Or l'habilitation
 * comme la résolution des destinataires reposent sur l'égalité exacte de ces chaînes : le préfixe
 * suffisait à ce qu'aucun titulaire ne soit reconnu, donc à ce que personne ne puisse décider ni
 * être prévenu.</p>
 *
 * <p>{@code WorkflowDataInitializer} ne recrée les circuits que sur une base vierge : sans cette
 * reprise, une installation déjà en service conserverait indéfiniment les anciennes valeurs.
 * L'opération est idempotente — au second démarrage, plus rien ne correspond au préfixe.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(110) // après WorkflowStepCodeInitializer (100), qui fiabilise les codes d'étape
public class RoleResponsableNormalizer implements CommandLineRunner {

    private static final String PREFIXE = "ROLE_";

    private final WorkflowRepository workflowRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Workflow> circuits = workflowRepository.findAll();
        int etapesCorrigees = 0;
        int transitionsCorrigees = 0;

        for (Workflow circuit : circuits) {
            boolean modifie = false;

            for (WorkflowStep etape : circuit.getSteps()) {
                String normalise = sansPrefixe(etape.getResponsableRole());
                if (normalise != null) {
                    log.info("Circuit « {} », étape « {} » : rôle responsable {} → {}",
                            circuit.getNom(), etape.getNomEtape(), etape.getResponsableRole(), normalise);
                    etape.setResponsableRole(normalise);
                    etapesCorrigees++;
                    modifie = true;
                }

                // Une transition peut porter sa propre habilitation, qui prime sur celle de
                // l'étape : la laisser préfixée rendrait le correctif partiel.
                for (WorkflowTransition transition : etape.getTransitions()) {
                    String habilitation = sansPrefixe(transition.getRequiredRole());
                    if (habilitation != null) {
                        transition.setRequiredRole(habilitation);
                        transitionsCorrigees++;
                        modifie = true;
                    }
                }
            }

            if (modifie) {
                // Horodatage explicite : la signature du catalogue repose sur la date de
                // modification du circuit, que la seule mise à jour de ses étapes ne touche pas.
                circuit.setUpdateAt(LocalDateTime.now());
                workflowRepository.save(circuit);
            }
        }

        if (etapesCorrigees > 0 || transitionsCorrigees > 0) {
            log.info("Normalisation des rôles responsables : {} étape(s) et {} transition(s) corrigées.",
                    etapesCorrigees, transitionsCorrigees);
        }
    }

    /** @return la valeur sans son préfixe, ou {@code null} si elle n'en portait pas. */
    private String sansPrefixe(String role) {
        if (role == null || !role.startsWith(PREFIXE)) {
            return null;
        }
        String normalise = role.substring(PREFIXE.length());
        return normalise.isBlank() ? null : normalise;
    }
}
