package com.qualiapproche.evaluation.service;

import java.util.List;
import java.util.UUID;

/**
 * Des constats viennent d'être validés en réunion d'équipe : publiés et verrouillés.
 *
 * <p>Tous, écarts ou non : le pilote du processus audité apprend ce que l'audit a retenu, pas
 * seulement ce qui part au module Non-conformités. Un geste, un événement — qu'il valide un
 * constat ou toute la compilation.</p>
 */
public record ConstatsPublies(UUID auditId, List<UUID> constatIds) {
}
