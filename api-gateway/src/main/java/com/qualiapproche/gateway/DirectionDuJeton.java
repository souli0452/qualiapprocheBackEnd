package com.qualiapproche.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * La direction (le client) que déclare le jeton d'une requête : le claim {@code direction_id}.
 *
 * <p>Lu sans vérifier la signature : la passerelle ne s'en sert que pour choisir la licence à
 * opposer à une écriture. Un jeton forgé pour se réclamer d'un autre client franchirait ce
 * contrôle, puis serait refusé par le service visé, qui vérifie la signature.</p>
 */
final class DirectionDuJeton {

    private static final ObjectMapper JSON = new ObjectMapper();

    private DirectionDuJeton() {
    }

    /** @return la direction du jeton, ou {@code null} s'il n'y en a pas ou qu'elle est illisible */
    static String lire(String autorisation) {
        if (autorisation == null || !autorisation.startsWith("Bearer ")) {
            return null;
        }
        String[] parties = autorisation.substring(7).trim().split("\\.");
        if (parties.length < 2) {
            return null;
        }
        try {
            JsonNode charge = JSON.readTree(new String(Base64.getUrlDecoder().decode(parties[1]),
                    StandardCharsets.UTF_8));
            String direction = charge.path("direction_id").asText(null);
            return direction == null || direction.isBlank() ? null : UUID.fromString(direction.trim()).toString();
        } catch (IllegalArgumentException | java.io.IOException e) {
            return null;
        }
    }
}
