package com.qualiapproche.common.cloisonnement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Joue les {@link Amorce} du service pour un client.
 *
 * <p>Toujours à la demande de referentiel-service, seul à connaître les clients : à la création
 * d'un client, puis à chacun de ses démarrages et chaque nuit pour tous — c'est ainsi qu'un
 * complément apporté par une nouvelle version (une permission de plus au SUPER_ADMIN, un modèle de
 * courriel) parvient à tous.</p>
 *
 * <p>Un service n'amorce jamais de lui-même ce qu'il croit être ses clients : avant le rattrapage,
 * sa colonne {@code direction_id} porte encore des identifiants de <b>structures</b>, et il
 * poserait un paramétrage en double sous chacune.</p>
 *
 * <p>Chaque amorce a sa propre transaction : l'échec de l'une n'emporte pas les autres, et le
 * rapport dit lesquelles ont échoué.</p>
 */
@Slf4j
public class AmorceDesDirections {

    private final ObjectProvider<Amorce> amorces;
    private final ExecutionCloisonnee execution;

    public AmorceDesDirections(ObjectProvider<Amorce> amorces, ExecutionCloisonnee execution) {
        this.amorces = amorces;
        this.execution = execution;
    }

    /** Amorce un client ; rend la liste des échecs (vide si tout est posé). */
    public List<String> amorcer(UUID direction) {
        if (!Cloisonnement.estUnClient(direction)) {
            throw new IllegalArgumentException("Direction invalide pour une amorce : " + direction);
        }
        List<String> echecs = new ArrayList<>();
        for (Amorce amorce : amorces.orderedStream().sorted(Comparator.comparingInt(Amorce::ordre)).toList()) {
            try {
                execution.pour(direction, amorce::amorcer);
            } catch (Exception e) {
                String nom = amorce.getClass().getSimpleName();
                log.error("Amorce {} échouée pour la direction {} : {}", nom, direction, e.getMessage(), e);
                echecs.add(nom + " : " + e.getMessage());
            }
        }
        return echecs;
    }
}
