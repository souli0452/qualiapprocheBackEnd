package com.qualiapproche.userservice.service;

import com.qualiapproche.common.cloisonnement.Cloisonnement;
import com.qualiapproche.userservice.config.ClaimStructureInitializer;
import com.qualiapproche.userservice.config.RolePlateformeInitializer;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rattache à leur client les comptes Keycloak créés avant le cloisonnement.
 *
 * <p>Un compte sans attribut {@code direction} n'a pas de claim {@code direction_id} : il ne voit
 * plus rien. referentiel-service, qui connaît l'arbre des structures, transmet la correspondance
 * structure → direction ; le compte reçoit la direction de sa structure, à défaut celle par défaut
 * (installation à un seul client), à défaut rien. Un compte déjà rattaché n'est jamais déplacé.
 * Les comptes de service et les administrateurs de la plateforme restent sans direction.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RattrapageDesComptes {

    private static final int LOT = 200;

    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;

    public Map<String, Object> rattacher(Map<UUID, UUID> structures, UUID parDefaut) {
        Map<UUID, UUID> correspondance = structures == null ? Map.of() : structures;
        UsersResource comptes = keycloak.realm(kcAuthProperties.getRealm()).users();
        int rattaches = 0;
        int dejaRattaches = 0;
        List<String> sansDirection = new ArrayList<>();
        for (int debut = 0; ; debut += LOT) {
            List<UserRepresentation> lot = comptes.list(debut, LOT);
            for (UserRepresentation compte : lot) {
                if (compte.getUsername() != null && compte.getUsername().startsWith("service-account-")) {
                    continue;
                }
                if (DirectionDesComptes.de(compte) != null) {
                    dejaRattaches++;
                    continue;
                }
                if (estAdministrateurDePlateforme(comptes, compte)) {
                    continue;
                }
                UUID structure = Cloisonnement.uuid(structureDe(compte));
                UUID direction = structure != null && correspondance.containsKey(structure)
                        ? correspondance.get(structure) : parDefaut;
                if (direction == null) {
                    sansDirection.add(compte.getUsername());
                    continue;
                }
                Map<String, List<String>> attributs = compte.getAttributes() == null
                        ? new HashMap<>() : new HashMap<>(compte.getAttributes());
                attributs.put(ClaimStructureInitializer.ATTRIBUT_DIRECTION, List.of(direction.toString()));
                compte.setAttributes(attributs);
                comptes.get(compte.getId()).update(compte);
                rattaches++;
            }
            if (lot.size() < LOT) {
                break;
            }
        }
        if (!sansDirection.isEmpty()) {
            log.warn("Comptes laissés sans direction (structure inconnue, aucune direction par défaut) : {}",
                    sansDirection);
        }
        log.info("Rattachement des comptes : {} rattaché(s), {} déjà rattaché(s), {} sans direction.",
                rattaches, dejaRattaches, sansDirection.size());
        Map<String, Object> rapport = new LinkedHashMap<>();
        rapport.put("rattaches", rattaches);
        rapport.put("dejaRattaches", dejaRattaches);
        rapport.put("sansDirection", String.join(", ", sansDirection));
        return rapport;
    }

    private static String structureDe(UserRepresentation compte) {
        if (compte.getAttributes() == null) {
            return null;
        }
        List<String> valeurs = compte.getAttributes().get("structure");
        return valeurs == null || valeurs.isEmpty() ? null : valeurs.get(0);
    }

    private static boolean estAdministrateurDePlateforme(UsersResource comptes, UserRepresentation compte) {
        try {
            return comptes.get(compte.getId()).roles().realmLevel().listAll().stream()
                    .anyMatch(r -> RolePlateformeInitializer.ROLE.equals(r.getName()));
        } catch (Exception e) {
            return false;
        }
    }
}
