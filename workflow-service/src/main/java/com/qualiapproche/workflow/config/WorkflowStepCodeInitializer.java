package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Complète les circuits antérieurs à l'introduction du code d'étape.
 *
 * <p>Le code est l'identité fonctionnelle d'une étape, mais les circuits déjà en base ont été
 * créés sans. Ce rattrapage leur en attribue un, dérivé du nom, une seule fois : les étapes qui
 * en possèdent déjà un ne sont jamais touchées, le code étant immuable.</p>
 *
 * <p>Exécuté après {@code WorkflowDataInitializer} pour que les circuits par défaut, créés au
 * premier démarrage, soient également couverts.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(100)
public class WorkflowStepCodeInitializer implements CommandLineRunner {

    private final WorkflowRepository workflowRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Workflow> circuits = workflowRepository.findAll();
        int complets = 0;

        for (Workflow circuit : circuits) {
            Set<String> codesUtilises = circuit.getSteps().stream()
                    .map(WorkflowStep::getCode)
                    .filter(c -> c != null && !c.isBlank())
                    .collect(java.util.stream.Collectors.toCollection(HashSet::new));

            boolean modifie = false;
            for (WorkflowStep step : circuit.getSteps()) {
                if (step.getCode() != null && !step.getCode().isBlank()) {
                    continue;
                }
                String code = codeDisponible(normaliser(step.getNomEtape()), codesUtilises);
                codesUtilises.add(code);
                step.setCode(code);
                modifie = true;
                complets++;
            }

            if (modifie) {
                workflowRepository.save(circuit);
                log.info("Codes d'étape attribués pour le circuit « {} »", circuit.getNom());
            }
        }

        if (complets > 0) {
            log.info("{} étape(s) sans code fonctionnel complétée(s).", complets);
        }
    }

    private String normaliser(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return "ETAPE";
        }
        String normalise = Normalizer.normalize(valeur, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (normalise.isBlank()) {
            return "ETAPE";
        }
        return normalise.length() > 60 ? normalise.substring(0, 60) : normalise;
    }

    private String codeDisponible(String base, Set<String> dejaPris) {
        if (!dejaPris.contains(base)) {
            return base;
        }
        for (int suffixe = 2; ; suffixe++) {
            String candidat = base + "_" + suffixe;
            if (!dejaPris.contains(candidat)) {
                return candidat;
            }
        }
    }
}
