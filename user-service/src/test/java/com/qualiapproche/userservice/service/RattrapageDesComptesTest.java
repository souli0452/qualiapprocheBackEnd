package com.qualiapproche.userservice.service;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RattrapageDesComptesTest {

    private static final UUID DIRECTION = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID SERVICE = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID PAR_DEFAUT = UUID.fromString("dddddddd-0000-0000-0000-000000000004");

    private final Keycloak keycloak = mock(Keycloak.class, RETURNS_DEEP_STUBS);
    private final UsersResource comptes = mock(UsersResource.class);

    private UserResource brancher(UserRepresentation u, String... rolesDeRoyaume) {
        UserResource ressource = mock(UserResource.class, RETURNS_DEEP_STUBS);
        when(ressource.roles().realmLevel().listAll()).thenReturn(java.util.Arrays.stream(rolesDeRoyaume)
                .map(nom -> new RoleRepresentation(nom, "", false)).toList());
        when(comptes.get(u.getId())).thenReturn(ressource);
        return ressource;
    }

    private static UserRepresentation compte(String id, String structure, String direction) {
        UserRepresentation u = new UserRepresentation();
        u.setId(id);
        u.setUsername(id);
        Map<String, List<String>> attributs = new HashMap<>();
        if (structure != null) {
            attributs.put("structure", List.of(structure));
        }
        if (direction != null) {
            attributs.put("direction", List.of(direction));
        }
        u.setAttributes(attributs);
        return u;
    }

    @Test
    @DisplayName("Chaque compte reçoit la direction de sa structure, à défaut celle par défaut ; rien n'est déplacé")
    void rattachement() {
        KcAuthProperties proprietes = new KcAuthProperties();
        proprietes.setRealm("quali");
        when(keycloak.realm("quali").users()).thenReturn(comptes);
        UserRepresentation duService = compte("agent", SERVICE.toString(), null);
        UserRepresentation orphelin = compte("orphelin", "inconnue", null);
        UserRepresentation dejaRattache = compte("deja", SERVICE.toString(), PAR_DEFAUT.toString());
        UserRepresentation plateforme = compte("plateforme", null, null);
        UserRepresentation service = compte("service-account-referentiel", null, null);
        when(comptes.list(0, 200)).thenReturn(List.of(duService, orphelin, dejaRattache, plateforme, service));
        UserResource agent = brancher(duService);
        UserResource sansStructure = brancher(orphelin);
        UserResource deja = brancher(dejaRattache);
        UserResource admin = brancher(plateforme, "PLATEFORME_ADMIN");

        Map<String, Object> rapport = new RattrapageDesComptes(keycloak, proprietes)
                .rattacher(Map.of(SERVICE, DIRECTION), PAR_DEFAUT);

        assertThat(rapport).containsEntry("rattaches", 2).containsEntry("dejaRattaches", 1);
        verify(agent).update(any());
        assertThat(duService.getAttributes().get("direction")).containsExactly(DIRECTION.toString());
        verify(sansStructure).update(any());
        assertThat(orphelin.getAttributes().get("direction")).containsExactly(PAR_DEFAUT.toString());
        verify(deja, never()).update(any());
        verify(admin, never()).update(any());
    }
}
