package com.qualiapproche.userservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.cloisonnement.Cloisonnement;
import com.qualiapproche.userservice.config.ClaimStructureInitializer;
import org.keycloak.representations.idm.UserRepresentation;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La direction (le client) d'un compte Keycloak, et ce que l'appelant a le droit d'en voir.
 *
 * <p>Keycloak est un royaume partagé par tous les clients : rien n'y cloisonne les comptes. Le
 * cloisonnement tient donc à l'attribut {@code direction} de chaque compte, comparé à la direction
 * de l'appelant ({@link Cloisonnement#courante()}) :</p>
 * <ul>
 *   <li>le système (racine) voit tous les comptes ;</li>
 *   <li>un client ne voit que les comptes de sa direction ;</li>
 *   <li>un appelant sans direction (l'administrateur de la plateforme) n'en voit aucun.</li>
 * </ul>
 */
public final class DirectionDesComptes {

    private static final ObjectMapper JSON = new ObjectMapper();

    private DirectionDesComptes() {
    }

    /** La direction inscrite sur le compte, ou {@code null}. */
    public static String de(UserRepresentation compte) {
        if (compte == null) {
            return null;
        }
        Map<String, List<String>> attributs = compte.getAttributes();
        if (attributs == null) {
            return null;
        }
        List<String> valeurs = attributs.get(ClaimStructureInitializer.ATTRIBUT_DIRECTION);
        return valeurs == null || valeurs.isEmpty() ? null : valeurs.get(0);
    }

    /** Vrai si l'appelant peut voir ce compte. */
    public static boolean visible(UserRepresentation compte) {
        UUID appelant = Cloisonnement.courante();
        if (Cloisonnement.estLaRacine(appelant)) {
            return true;
        }
        if (!Cloisonnement.estUnClient(appelant)) {
            return false;
        }
        return appelant.toString().equals(de(compte));
    }

    /**
     * La direction que porte un jeton d'accès que Keycloak vient de rendre, lue sans revérifier sa
     * signature : il n'a pas quitté le serveur. À ne jamais employer pour un jeton reçu d'un client.
     */
    public static UUID duJetonFraichementEmis(String jetonDAcces) {
        if (jetonDAcces == null) {
            return null;
        }
        String[] parties = jetonDAcces.split("\\.");
        if (parties.length < 2) {
            return null;
        }
        try {
            JsonNode corps = JSON.readTree(new String(Base64.getUrlDecoder().decode(parties[1]),
                    StandardCharsets.UTF_8));
            JsonNode direction = corps.get(Cloisonnement.CLAIM);
            return direction == null ? null : Cloisonnement.uuid(direction.asText());
        } catch (Exception e) {
            return null;
        }
    }
}
