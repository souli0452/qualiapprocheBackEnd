package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deux pièges silencieux de l'habilitation par annotation, vérifiés sur tous les contrôleurs.
 *
 * <p>{@code @PreAuthorize("@perm.canRead(this)")} interroge l'annotation de classe
 * {@link RequirePermissions}. Absente, {@code PermissionChecker} ne refuse pas l'accès : il
 * répond {@code false} à tout, et <b>chaque</b> endpoint du contrôleur renvoie 403 — sans que
 * rien ne le signale à la compilation ni au démarrage.</p>
 *
 * <p>Un contrôleur déclaré {@code record} est implicitement final : la sécurité au niveau des
 * méthodes s'applique par un proxy CGLIB, qui ne peut pas sous-classer une classe finale. Le
 * cas s'est présenté sur {@code StructureController} de referentiel-service.</p>
 */
class HabilitationDesControleursTest {

    private static final String PAQUET = "com.qualiapproche.amelioration.controller";

    private List<Class<?>> controleursGardes() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Class<?>> gardes = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(PAQUET)) {
            try {
                Class<?> type = Class.forName(definition.getBeanClassName());
                for (Method methode : type.getDeclaredMethods()) {
                    if (methode.isAnnotationPresent(PreAuthorize.class)) {
                        gardes.add(type);
                        break;
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return gardes;
    }

    @Test
    @DisplayName("Tout contrôleur portant @PreAuthorize déclare aussi @RequirePermissions")
    void controleursGardes_declarentLeursPermissions() {
        List<Class<?>> gardes = controleursGardes();
        assertThat(gardes).isNotEmpty();

        assertThat(gardes)
                .allSatisfy(type -> assertThat(type.getAnnotation(RequirePermissions.class))
                        .withFailMessage("%s garde ses méthodes par @PreAuthorize mais ne déclare "
                                + "aucune permission : tous ses endpoints répondraient 403.",
                                type.getSimpleName())
                        .isNotNull());
    }

    @Test
    @DisplayName("Aucun contrôleur gardé n'est final : la sécurité de méthode exige un proxy")
    void controleursGardes_sontProxifiables() {
        assertThat(controleursGardes())
                .allSatisfy(type -> assertThat(Modifier.isFinal(type.getModifiers()))
                        .withFailMessage("%s est final (record ?) : CGLIB ne peut pas le "
                                + "sous-classer, le contexte refuserait de démarrer.",
                                type.getSimpleName())
                        .isFalse());
    }

    @Test
    @DisplayName("Les permissions déclarées suivent le nommage <ressource>-<action>")
    void permissionsDeclarees_suiventLeNommage() {
        // Les anciens noms en majuscules restent acceptés le temps de la migration ; toute
        // permission nouvelle doit en revanche adopter le nommage retenu (document-read…).
        for (Class<?> type : controleursGardes()) {
            RequirePermissions declaration = type.getAnnotation(RequirePermissions.class);
            Set<String> noms = new java.util.HashSet<>();
            noms.addAll(Set.of(declaration.create()));
            noms.addAll(Set.of(declaration.update()));
            noms.addAll(Set.of(declaration.read()));
            noms.addAll(Set.of(declaration.delete()));
            noms.addAll(Set.of(declaration.validate()));

            assertThat(noms)
                    .withFailMessage("%s ne déclare aucune permission.", type.getSimpleName())
                    .isNotEmpty();
            assertThat(noms.stream().filter(n -> !n.equals(n.toUpperCase())))
                    .withFailMessage("%s : nommage attendu <ressource>-<action> en minuscules.",
                            type.getSimpleName())
                    .allMatch(n -> n.matches("[a-z0-9]+(-[a-z0-9]+)+"));
        }
    }
}
