package com.qualiapproche.userservice.config;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Installe sur le client Keycloak le mapper qui porte la structure de l'utilisateur dans son jeton.
 *
 * <p>La structure vit en attribut Keycloak ({@code structure}), et les services la lisent dans le
 * claim {@code structure_id} du jeton ({@code SecurityUtils.getCurrentUserStructureId()}) — mais
 * aucun mapper ne reliait l'un à l'autre : le claim n'existait pas, la méthode rendait {@code null}
 * pour tout le monde, et tout ce qui s'y adosse — la structure d'origine d'un dossier de workflow,
 * le filtrage des listes de non-conformités — restait silencieusement sans effet.</p>
 *
 * <p>Posé ici plutôt que configuré à la main dans la console : la configuration suit le code sur
 * chaque environnement, et survit à une recréation du royaume. Idempotent — un mapper déjà présent,
 * qu'il vienne d'un démarrage précédent ou de la console, est laissé tel quel.</p>
 *
 * <p>Un échec n'empêche pas le service de démarrer : workflow-service sait résoudre la structure en
 * interrogeant user-service quand le jeton ne la porte pas ({@code StructureUtilisateurService}).
 * Le claim ne vaut que pour les jetons émis après son installation — les sessions ouvertes gardent
 * le leur jusqu'à reconnexion.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimStructureInitializer implements CommandLineRunner {

    /** Attribut Keycloak où user-service inscrit la structure (cf. KcUserService.createUser). */
    static final String ATTRIBUT = "structure";

    /** Claim que lisent les services (cf. SecurityUtils.getCurrentUserStructureId). */
    static final String CLAIM = "structure_id";

    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;

    @Override
    public void run(String... args) {
        try {
            List<ClientRepresentation> clients = keycloak.realm(kcAuthProperties.getRealm())
                    .clients().findByClientId(kcAuthProperties.getClientId());
            if (clients.isEmpty()) {
                log.warn("Client Keycloak « {} » introuvable dans le royaume « {} » : le claim {} "
                                + "n'est pas installé. Les services compensent en interrogeant user-service.",
                        kcAuthProperties.getClientId(), kcAuthProperties.getRealm(), CLAIM);
                return;
            }

            ProtocolMappersResource mappers = keycloak.realm(kcAuthProperties.getRealm())
                    .clients().get(clients.get(0).getId()).getProtocolMappers();

            boolean dejaPresent = mappers.getMappers().stream()
                    .anyMatch(mapper -> mapper.getConfig() != null
                            && CLAIM.equals(mapper.getConfig().get("claim.name")));
            if (dejaPresent) {
                log.info("Le claim {} est déjà mappé sur le client « {} » : rien à faire.",
                        CLAIM, kcAuthProperties.getClientId());
                return;
            }

            try (Response reponse = mappers.createMapper(mapperStructure())) {
                if (reponse.getStatus() == Response.Status.CREATED.getStatusCode()) {
                    log.info("Claim {} installé sur le client « {} » : les jetons émis désormais "
                                    + "portent la structure de l'utilisateur.",
                            CLAIM, kcAuthProperties.getClientId());
                } else {
                    log.warn("Le mapper du claim {} n'a pas été créé (HTTP {}). Les services "
                                    + "compensent en interrogeant user-service.",
                            CLAIM, reponse.getStatus());
                }
            }
        } catch (Exception e) {
            // Keycloak injoignable ou droits insuffisants : le service démarre quand même — le
            // repli par interrogation de user-service reste en place côté workflow.
            log.warn("Installation du claim {} impossible : {}. Les services compensent en "
                    + "interrogeant user-service.", CLAIM, e.getMessage());
        }
    }

    /** Le mapper tel que la console l'aurait créé : « User Attribute », attribut → claim. */
    static ProtocolMapperRepresentation mapperStructure() {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("structure vers structure_id");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usermodel-attribute-mapper");

        Map<String, String> config = new HashMap<>();
        config.put("user.attribute", ATTRIBUT);
        config.put("claim.name", CLAIM);
        config.put("jsonType.label", "String");
        // Le jeton d'accès est celui que les services décodent ; l'ID token et userinfo suivent,
        // pour que le frontal puisse lire la même information sans autre appel.
        config.put("access.token.claim", "true");
        config.put("id.token.claim", "true");
        config.put("userinfo.token.claim", "true");
        mapper.setConfig(config);
        return mapper;
    }
}
