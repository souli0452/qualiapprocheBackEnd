package com.qualiapproche.workflow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Réglages publics de l'organisation, détenus par referentiel-service.
 *
 * <p>Contact, téléphone, logo, adresse : de quoi composer le pied de page des courriels d'étape.
 * Le moteur ne connaît pas ces valeurs et n'a pas à les connaître — il demande une carte de clés et
 * de valeurs, et l'organisation en ajoute sans qu'on livre du code.</p>
 *
 * <p>La réponse est lue en {@code Map} : referentiel-service encapsule ses réponses dans
 * {@code ApiResponse}, enveloppe que ce service ne partage pas.</p>
 */
@FeignClient(name = "referentiel-service")
public interface ParametreClient {

    @GetMapping("/api/v1/parametres/publics")
    Map<String, Object> valeursPubliques();
}
