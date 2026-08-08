package com.qualiapproche.userservice.config;

import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La dotation du super administrateur.
 *
 * <p>Les listes de permissions ne sont volontairement pas réappliquées à chaque démarrage : un
 * droit accordé depuis l'écran d'administration disparaissait sinon au redémarrage suivant, sans
 * trace ni message. Une seule permission fait exception, et ces cas disent laquelle et
 * pourquoi.</p>
 */
class RoleInitializerTest {

    private static final String LICENCE = "licence-write";

    private AppRoleRepository repository;
    private RoleInitializer initialiseur;

    @BeforeEach
    void setUp() {
        repository = mock(AppRoleRepository.class);
        // Tous les rôles standards existent déjà — le cas d'une installation en service. Seul le
        // rattrapage nous intéresse ici, et c'est alors le seul enregistrement possible.
        when(repository.findByName(anyString())).thenReturn(Optional.of(AppRole.builder()
                .name("rôle déjà en base")
                .permissions(new ArrayList<>())
                .build()));
        initialiseur = new RoleInitializer(repository);
    }

    private AppRole superAdmin(String... permissions) {
        AppRole role = AppRole.builder()
                .name("SUPER_ADMIN")
                .description("Accès total")
                .permissions(new ArrayList<>(List.of(permissions)))
                .build();
        when(repository.findByName("SUPER_ADMIN")).thenReturn(Optional.of(role));
        return role;
    }

    @Test
    @DisplayName("Un SUPER_ADMIN privé du droit de poser une licence le retrouve au démarrage")
    void permissionManquante_estRendue() {
        // Le cas de toute installation créée avant que cette permission n'existe au dictionnaire :
        // sans elle, plus personne ne peut installer de licence ni démarrer d'essai, et une
        // installation échue reste définitivement en lecture seule.
        superAdmin("nc-read", "structure-write");

        initialiseur.run();

        ArgumentCaptor<AppRole> enregistre = ArgumentCaptor.forClass(AppRole.class);
        verify(repository).save(enregistre.capture());
        assertThat(enregistre.getValue().getPermissions())
                .contains(LICENCE)
                .contains("nc-read", "structure-write");
    }

    @Test
    @DisplayName("Rien n'est réécrit quand la permission est déjà là")
    void permissionPresente_riennEstReecrit() {
        superAdmin("nc-read", LICENCE);

        initialiseur.run();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Le reste de la dotation n'est pas rétabli : le rôle appartient à l'administrateur")
    void autresPermissions_nesontPasRetablies() {
        // Un administrateur qui a délibérément retiré des droits au rôle les retrouverait sinon
        // au redémarrage, sans le savoir.
        superAdmin(LICENCE);

        initialiseur.run();

        verify(repository, never()).save(any());
    }
}
