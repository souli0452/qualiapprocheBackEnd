package com.qualiapproche.userservice.config;

import com.qualiapproche.common.utils.PermissionsPortee;
import com.qualiapproche.common.utils.PermissionsTableauDeBord;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ce qu'un démarrage ajoute aux rôles déjà en base, et ce qu'il n'y touche pas.
 *
 * <p>Les listes de permissions ne sont volontairement pas réappliquées à chaque démarrage : un
 * droit accordé depuis l'écran d'administration disparaissait sinon au redémarrage suivant, sans
 * trace ni message. Les seules exceptions sont des ajouts <b>nommés</b>, et ces cas disent
 * lesquels et pourquoi — une permission nouvelle n'atteindrait sans cela aucune installation en
 * service, et l'écran qu'elle protège se refermerait sur tout le monde le jour de la mise à
 * jour.</p>
 */
class RoleInitializerTest {

    private static final String LICENCE = "licence-write";

    private static final String[] TABLEAUX_DE_BORD = {
            PermissionsTableauDeBord.PERSONNEL,
            PermissionsTableauDeBord.STRUCTURE,
            PermissionsTableauDeBord.ORGANISME};

    /** Les portées, qui ont remplacé les listes de noms de rôles écrites dans le code. */
    private static final String[] PORTEE = {
            PermissionsPortee.TOUTES_STRUCTURES,
            PermissionsPortee.DECIDER_PARTOUT,
            PermissionsPortee.HORS_CLASSEMENT};

    /** Ce que tout rôle déjà à jour porte : le rattrapage n'a alors rien à faire. */
    private static List<String> aJour() {
        List<String> tout = new ArrayList<>(List.of(TABLEAUX_DE_BORD));
        tout.addAll(List.of(PORTEE));
        return tout;
    }

    private AppRoleRepository repository;
    private RoleInitializer initialiseur;

    @BeforeEach
    void setUp() {
        repository = mock(AppRoleRepository.class);
        // Tous les rôles standards existent déjà — le cas d'une installation en service. Seul le
        // rattrapage nous intéresse ici, et c'est alors le seul enregistrement possible. Ils sont
        // rendus à jour de leurs tableaux de bord, pour qu'un cas ne réponde que de ce qu'il pose.
        when(repository.findByName(anyString())).thenReturn(Optional.of(AppRole.builder()
                .name("rôle déjà en base")
                .permissions(new ArrayList<>(aJour()))
                .build()));
        initialiseur = new RoleInitializer(repository);
    }

    /** Un SUPER_ADMIN déjà en base, à jour de ses tableaux de bord sauf mention contraire. */
    private AppRole superAdmin(String... permissions) {
        List<String> dotation = aJour();
        dotation.addAll(List.of(permissions));
        return role("SUPER_ADMIN", dotation);
    }

    private AppRole role(String nom, List<String> permissions) {
        AppRole role = AppRole.builder()
                .name(nom)
                .description("Rôle déjà en base")
                .permissions(new ArrayList<>(permissions))
                .build();
        when(repository.findByName(nom)).thenReturn(Optional.of(role));
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

    @Test
    @DisplayName("Un rôle existant reçoit la portée de tableau de bord qui est la sienne")
    void tableauxDeBord_ouvertsAuxRolesExistants() {
        // Le cas de toute installation antérieure à ces permissions : les dotations ci-dessus ne
        // valent que pour un rôle créé, si bien que la nouveauté n'atteindrait personne et que
        // l'écran se refermerait sur tout le monde.
        AppRole agent = role("AGENT", List.of("nc-read"));
        AppRole pilote = role("PILOTE", List.of("nc-read"));
        AppRole qualite = role("RESPONSABLE_QUALITE", List.of("nc-read"));

        initialiseur.run();

        // Chacun reçoit exactement sa portée, et pas celle du voisin : l'agent ne gagne pas la vue
        // d'ensemble de l'organisme en passant par ce chemin.
        assertThat(agent.getPermissions())
                .contains("nc-read", PermissionsTableauDeBord.PERSONNEL)
                .doesNotContain(PermissionsTableauDeBord.STRUCTURE,
                        PermissionsTableauDeBord.ORGANISME);
        assertThat(pilote.getPermissions())
                .contains(PermissionsTableauDeBord.PERSONNEL, PermissionsTableauDeBord.STRUCTURE)
                .doesNotContain(PermissionsTableauDeBord.ORGANISME);
        assertThat(qualite.getPermissions()).contains(TABLEAUX_DE_BORD);
    }

    @Test
    @DisplayName("Un rôle existant reçoit la portée qui remplaçait les tests sur son nom")
    void portee_donneeAuxRolesExistants() {
        // Sans ce rattrapage, la bascule des noms de rôles vers les permissions serait une
        // régression silencieuse : le jour de la mise à jour, un responsable qualité déjà en base
        // cesserait de voir les dossiers des autres structures.
        AppRole qualite = role("RESPONSABLE_QUALITE", List.of("nc-read"));
        AppRole admin = role("SUPER_ADMIN", List.of(LICENCE));

        initialiseur.run();

        // Voir n'est pas décider : le responsable qualité ne gagne pas le droit de décider partout
        // en passant par ce chemin, ni celui de passer outre le classement des documents.
        assertThat(qualite.getPermissions())
                .contains("nc-read", PermissionsPortee.TOUTES_STRUCTURES)
                .doesNotContain(PermissionsPortee.DECIDER_PARTOUT,
                        PermissionsPortee.HORS_CLASSEMENT);
        assertThat(admin.getPermissions()).contains(PORTEE);
    }

    @Test
    @DisplayName("Un rôle déjà à jour n'est pas réécrit")
    void tableauxDeBordPresents_rienNestReecrit() {
        superAdmin(LICENCE);
        role("AGENT", List.of(PermissionsTableauDeBord.PERSONNEL));

        initialiseur.run();

        // Un démarrage sur une base à jour ne touche aucune ligne.
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Les trois permissions figurent au dictionnaire servi à l'écran des rôles")
    void tableauxDeBord_declaresAuDictionnaire() throws Exception {
        // Le nom est écrit à trois endroits — dictionnaire, dotation, protection du point d'entrée.
        // Une divergence entre deux d'entre eux ne se manifeste que par un refus d'accès, sans rien
        // qui l'explique : l'écran d'attribution ne proposerait pas un droit que le serveur exige.
        List<Map<String, String>> dictionnaire;
        try (java.io.InputStream flux =
                     new org.springframework.core.io.ClassPathResource("permissions.json").getInputStream()) {
            dictionnaire = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(flux, new com.fasterxml.jackson.core.type.TypeReference<>() { });
        }

        assertThat(dictionnaire).extracting(entree -> entree.get("value"))
                .contains(TABLEAUX_DE_BORD);
    }
}
