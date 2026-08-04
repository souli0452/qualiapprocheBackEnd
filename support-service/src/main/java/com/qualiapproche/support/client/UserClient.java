package com.qualiapproche.support.client;

import com.qualiapproche.support.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Profil d'un utilisateur, détenu par user-service.
 *
 * <p>La visibilité d'un document repose sur la structure de son auteur et sur les rôles de qui le
 * consulte. Or ni le jeton Keycloak — qui ne porte que des rôles techniques — ni l'en-tête
 * {@code X-User-Permissions} — qui ne transporte que des permissions — ne disent à quelle structure
 * appartient l'appelant : cet attribut vit dans Keycloak sous le nom {@code structure}, et
 * user-service est seul à le servir.</p>
 *
 * <p>{@code /me} ne peut pas être employé ici : il lit le jeton dans le cookie de la requête, que
 * l'appel de service à service ne porte pas. {@code /user-by-id} prend l'identifiant en paramètre
 * et s'accommode de l'en-tête {@code Authorization} que propage {@link FeignConfig}.</p>
 */
@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {

    /**
     * @return le profil : {@code structure} (identifiant de la structure de rattachement),
     *         {@code roles} et {@code appRoles} (rôles applicatifs), entre autres.
     */
    @GetMapping("/api/v1/user-by-id")
    Map<String, Object> getUserById(@RequestParam("userId") String userId);
}
