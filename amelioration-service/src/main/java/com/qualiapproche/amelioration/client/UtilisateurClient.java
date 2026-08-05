package com.qualiapproche.amelioration.client;

import com.qualiapproche.amelioration.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Identité d'un utilisateur désigné par le circuit.
 *
 * <p>Une ré-attribution de plan d'action ne transmet qu'un identifiant : le moteur ne transporte
 * que des chaînes. Sans cette lecture, la fiche affichait le <b>nouvel</b> identifiant sous
 * l'<b>ancien</b> nom — le pire des deux, puisque plus rien ne disait qui répondait de l'action.</p>
 *
 * <p>La réponse est lue en {@code Map} : user-service encapsule ses réponses dans une enveloppe que
 * ce service ne partage pas.</p>
 */
@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UtilisateurClient {

    @GetMapping("/api/v1/user-by-id")
    Map<String, Object> getUserById(@RequestParam("userId") String userId);
}
