package com.qualiapproche.userservice.config;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Installation du claim {@code structure_id} dans les jetons.
 *
 * <p>C'est le maillon qui manquait à tout le filtrage par structure : l'attribut existait côté
 * Keycloak, le claim était lu côté services, et rien ne reliait les deux — silencieusement.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimStructureInitializerTest {

    @Mock private Keycloak keycloak;
    @Mock private RealmResource realm;
    @Mock private ClientsResource clients;
    @Mock private ClientResource client;
    @Mock private ProtocolMappersResource mappers;

    private ClaimStructureInitializer initialiseur;

    @BeforeEach
    void setUp() {
        KcAuthProperties proprietes = new KcAuthProperties();
        proprietes.setRealm("quali");
        proprietes.setClientId("quali-sira");
        initialiseur = new ClaimStructureInitializer(keycloak, proprietes);

        ClientRepresentation representation = new ClientRepresentation();
        representation.setId("uuid-client");
        representation.setClientId("quali-sira");

        when(keycloak.realm("quali")).thenReturn(realm);
        when(realm.clients()).thenReturn(clients);
        when(clients.findByClientId("quali-sira")).thenReturn(List.of(representation));
        when(clients.get("uuid-client")).thenReturn(client);
        when(client.getProtocolMappers()).thenReturn(mappers);
        when(mappers.createMapper(any(ProtocolMapperRepresentation.class))).thenReturn(Response.status(Response.Status.CREATED).build());
    }

    @Test
    @DisplayName("Le mapper est créé avec l'attribut, le claim et le jeton d'accès attendus")
    void mapperAbsent_cree() {
        when(mappers.getMappers()).thenReturn(List.of());

        initialiseur.run();

        ArgumentCaptor<ProtocolMapperRepresentation> capture =
                ArgumentCaptor.forClass(ProtocolMapperRepresentation.class);
        verify(mappers).createMapper(capture.capture());

        Map<String, String> config = capture.getValue().getConfig();
        // C'est l'égalité exacte de ces trois valeurs qui relie l'attribut écrit par
        // KcUserService au claim lu par SecurityUtils — et au jeton que les services décodent.
        assertThat(config.get("user.attribute")).isEqualTo("structure");
        assertThat(config.get("claim.name")).isEqualTo("structure_id");
        assertThat(config.get("access.token.claim")).isEqualTo("true");
        assertThat(capture.getValue().getProtocolMapper()).isEqualTo("oidc-usermodel-attribute-mapper");
    }

    @Test
    @DisplayName("Un mapper déjà en place — console ou démarrage précédent — est laissé tel quel")
    void mapperPresent_rienAFaire() {
        ProtocolMapperRepresentation existant = new ProtocolMapperRepresentation();
        existant.setName("posé à la main depuis la console");
        existant.setConfig(Map.of("claim.name", "structure_id", "user.attribute", "structure"));
        when(mappers.getMappers()).thenReturn(List.of(existant));

        initialiseur.run();

        // Le reconnaître au claim et non au nom : un administrateur qui l'a créé depuis la
        // console ne l'a pas nommé comme nous, et un doublon écraserait sa configuration.
        verify(mappers, never()).createMapper(any(ProtocolMapperRepresentation.class));
    }

    @Test
    @DisplayName("Client introuvable : rien n'est créé, et le démarrage n'échoue pas")
    void clientIntrouvable_demarrageIntact() {
        when(clients.findByClientId("quali-sira")).thenReturn(List.of());

        assertThatCode(() -> initialiseur.run()).doesNotThrowAnyException();
        verify(mappers, never()).createMapper(any(ProtocolMapperRepresentation.class));
    }

    @Test
    @DisplayName("Keycloak injoignable : le service démarre quand même — le repli compense")
    void keycloakInjoignable_demarrageIntact() {
        doThrow(new IllegalStateException("connexion refusée")).when(keycloak).realm("quali");

        // workflow-service sait résoudre la structure en interrogeant user-service : un royaume
        // indisponible au démarrage ne doit pas empêcher le service de servir le reste.
        assertThatCode(() -> initialiseur.run()).doesNotThrowAnyException();
    }
}
