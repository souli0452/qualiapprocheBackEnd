package com.qualiapproche.workflow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Rôles applicatifs de l'utilisateur connecté, détenus par user-service.
 *
 * <p>Le contrôle d'habilitation d'une étape porte sur un rôle, désigné par son identifiant. Or ni
 * le jeton Keycloak — qui ne connaît que des rôles techniques — ni l'en-tête
 * {@code X-User-Permissions} — qui ne transporte que des permissions — ne permettent de savoir
 * quels rôles porte l'appelant. Cette information est donc demandée à sa source.</p>
 *
 * <p>La réponse est lue en {@code Map} : user-service encapsule ses réponses dans
 * {@code ApiResponse}, enveloppe que ce service ne partage pas.</p>
 */
@FeignClient(name = "user-service")
public interface UserRoleClient {

    @GetMapping("/api/v1/me/roles")
    Map<String, Object> getMyRoles();
}
