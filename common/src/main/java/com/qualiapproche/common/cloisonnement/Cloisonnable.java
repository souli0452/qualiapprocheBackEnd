package com.qualiapproche.common.cloisonnement;

import java.util.UUID;

/**
 * Une entité qui appartient à un client : elle porte sa direction dans une colonne
 * {@code direction_id} annotée {@code @TenantId}. {@code AuditEntity} l'est ; une entité qui n'en
 * hérite pas mais qui est à un client (un dossier du moteur de workflow) le déclare elle-même.
 */
public interface Cloisonnable {

    UUID getDirectionId();
}
