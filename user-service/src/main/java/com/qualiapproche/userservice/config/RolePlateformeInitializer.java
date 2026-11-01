package com.qualiapproche.userservice.config;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Crée dans Keycloak le rôle de royaume {@code PLATEFORME_ADMIN} s'il manque.
 *
 * <p>Il désigne l'administrateur de la plateforme : celui qui crée les clients, au-dessus d'eux,
 * sans direction et donc sans accès à leurs données. Rôle de royaume et non rôle applicatif : les
 * rôles applicatifs appartiennent chacun à un client. Comme {@link ClaimStructureInitializer}, un
 * Keycloak injoignable n'empêche pas le service de démarrer.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RolePlateformeInitializer implements CommandLineRunner {

    public static final String ROLE = "PLATEFORME_ADMIN";

    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;

    @Override
    public void run(String... args) {
        try {
            RolesResource roles = keycloak.realm(kcAuthProperties.getRealm()).roles();
            boolean present = roles.list().stream().anyMatch(r -> ROLE.equals(r.getName()));
            if (present) {
                return;
            }
            RoleRepresentation role = new RoleRepresentation();
            role.setName(ROLE);
            role.setDescription("Administrateur de la plateforme : crée les clients, ne lit pas leurs données");
            roles.create(role);
            log.info("Rôle de royaume {} créé.", ROLE);
        } catch (Exception e) {
            log.warn("Rôle de royaume {} non vérifié : {}. La création d'administrateurs de plateforme "
                    + "échouera tant qu'il manque.", ROLE, e.getMessage());
        }
    }
}
