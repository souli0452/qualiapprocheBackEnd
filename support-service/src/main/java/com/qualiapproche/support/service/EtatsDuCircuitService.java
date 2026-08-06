package com.qualiapproche.support.service;

import com.qualiapproche.common.dto.WorkflowStateDto;
import com.qualiapproche.support.client.WorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * État du circuit de plusieurs dossiers, en un minimum d'appels.
 *
 * <p>Une liste de travail doit porter les actions ouvertes sur chaque ligne, sans quoi elle se
 * borne à annoncer qu'il y a quelque chose à faire ailleurs. Les demander dossier par dossier
 * multipliait les allers-retours autant que de lignes affichées ; le moteur sait répondre par lot.</p>
 *
 * <p>Deux précautions, apprises du module non-conformité qui a posé la même liste avant celui-ci :
 * le lot est découpé, parce qu'au-delà d'un seuil le moteur refuse la demande <b>entière</b> et
 * toutes les actions seraient perdues d'un coup ; et un moteur indisponible rend une carte vide
 * plutôt qu'une erreur, la liste s'affichant alors sans ses boutons — ce qui vaut mieux qu'un écran
 * en échec.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EtatsDuCircuitService {

    /**
     * Taille maximale d'un lot demandé au moteur, alignée sur ce qu'il accepte.
     *
     * <p>Au-delà, il refuse la demande entière : une liste plus longue que ce seuil aurait donc
     * perdu la totalité de ses actions, et silencieusement.</p>
     */
    private static final int TAILLE_LOT = 200;

    private final WorkflowClient workflowClient;

    /** États indexés par ressource. Les dossiers sans circuit en cours n'y figurent simplement pas. */
    public Map<UUID, WorkflowStateDto> pourRessources(List<UUID> identifiants) {
        Map<UUID, WorkflowStateDto> etats = new HashMap<>();
        if (identifiants == null || identifiants.isEmpty()) {
            return etats;
        }

        List<UUID> aDemander = identifiants.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        for (int debut = 0; debut < aDemander.size(); debut += TAILLE_LOT) {
            List<UUID> lot = aDemander.subList(debut, Math.min(debut + TAILLE_LOT, aDemander.size()));
            try {
                Map<UUID, WorkflowStateDto> reponse = workflowClient.getWorkflowStates(lot);
                if (reponse != null) {
                    etats.putAll(reponse);
                }
            } catch (Exception e) {
                log.warn("États de circuit indisponibles pour {} dossier(s) : {}", lot.size(), e.getMessage());
            }
        }
        return etats;
    }

    /**
     * Ressources de ce type sur lesquelles l'appelant a une décision ouverte.
     *
     * <p>Le moteur hors d'atteinte rend une liste vide : une liste de travail momentanément vide,
     * qui se remplira au rétablissement, vaut mieux qu'un écran en erreur.</p>
     */
    public List<UUID> ressourcesADecider(String typeRessource) {
        try {
            List<UUID> aDecider = workflowClient.ressourcesADecider(typeRessource);
            return aDecider != null ? aDecider : List.of();
        } catch (Exception e) {
            log.warn("Liste « à traiter » ({}) indisponible, le service de circuits est injoignable : {}",
                    typeRessource, e.getMessage());
            return List.of();
        }
    }
}
