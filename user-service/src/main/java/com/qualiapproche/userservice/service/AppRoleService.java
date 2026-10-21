package com.qualiapproche.userservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.UserRoleAssignment;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Création et modification des rôles applicatifs, sous la règle qui fait du <b>nom</b> du rôle son
 * identité fonctionnelle.
 *
 * <p>Ce n'est pas un choix pris ici : c'est déjà le nom que désignent les étapes des circuits
 * ({@code WorkflowStep.responsableRole}), les entrées du catalogue d'étapes, la resynchronisation
 * des rôles standard au démarrage ({@code RoleInitializer.syncRole}), le calcul des permissions
 * ({@code KcUserService.computePermissions}) et la résolution des destinataires d'une notification
 * ({@code getUsersByRole}). Tous reposent sur l'égalité exacte de cette chaîne, et aucun ne connaît
 * l'identifiant technique du rôle.</p>
 *
 * <p>D'où les deux règles appliquées ici, sans lesquelles le nom ne peut pas tenir ce rôle :</p>
 *
 * <ul>
 *   <li><b>Le nom ne se modifie pas.</b> Le point d'entrée était un {@code save()} sur l'entité
 *       reçue : renommer un rôle depuis l'écran d'administration était donc possible, et se payait
 *       en cascade — plus aucun titulaire habilité à décider sur les étapes qui le désignaient,
 *       plus aucun destinataire à notifier, et pour un rôle standard, sa recréation sous l'ancien
 *       nom au démarrage suivant, laissant deux rôles là où l'administrateur croyait n'en avoir
 *       qu'un. Rien de tout cela n'était signalé : le renommage réussissait.</li>
 *   <li><b>Deux rôles ne peuvent pas porter le même nom.</b> Un homonyme rendrait ambiguë chacune
 *       de ces résolutions, et ferait lever {@code findByName}.</li>
 * </ul>
 *
 * <p>Troisième règle : <b>un rôle ne porte que des permissions du dictionnaire</b>
 * ({@code permissions.json}, celui que l'écran propose). Le point d'entrée acceptait n'importe
 * quelle chaîne : {@code SUPER_ADMIN}, {@code SERVICE_TECHNIQUE} ou une permission d'un autre
 * service se forgeaient, puis la passerelle les relayait à tous les services.</p>
 *
 * <p>Quatrième règle : <b>nul ne met dans un rôle une permission qu'il ne détient pas</b>, ni ne
 * l'en retire ({@link GardeDesHabilitations}). Sans elle, un gestionnaire des rôles ajoutait au
 * sien la permission de son choix et la recevait à sa prochaine requête.</p>
 *
 * <p>Le libellé reste donc figé une fois publié, comme le code d'une étape ou d'une entrée de
 * catalogue. La description, elle, se modifie librement : c'est là que se corrige la formulation
 * présentée à l'écran d'attribution.</p>
 */
@Service
@RequiredArgsConstructor
public class AppRoleService {

    private static final String DICTIONNAIRE = "permissions.json";

    /**
     * Noms qu'aucun rôle ne peut recevoir par l'écran d'administration, quelle que soit leur casse.
     *
     * <p>{@link GardeDesHabilitations} reconnaît le SUPER_ADMIN à son nom, sans égard à la casse,
     * et ne le borne en rien. La création, elle, ne refusait que l'homonyme exact : un gestionnaire
     * des rôles créait {@code super_admin} avec les seules permissions qu'il détenait, se le faisait
     * attribuer par un collègue, et devenait administrateur total. Le rôle {@code SUPER_ADMIN}
     * légitime est posé au démarrage par {@code RoleInitializer}, qui ne passe pas par ici ;
     * {@code SERVICE_TECHNIQUE} est refusé comme côté Keycloak ({@link KcRoleService}).</p>
     */
    private static final Set<String> NOMS_RESERVES = Set.of("SUPER_ADMIN", "SUPERADMIN", "SERVICE_TECHNIQUE");

    private final AppRoleRepository appRoleRepository;
    private final UserRoleAssignmentRepository affectations;
    private final GardeDesHabilitations garde;

    /** Les valeurs du dictionnaire, lues une fois. */
    private volatile Set<String> permissionsConnues;

    @Transactional
    public AppRole enregistrer(AppRole role) {
        String nom = role.getName() == null ? null : role.getName().trim();
        if (nom == null || nom.isBlank()) {
            throw new BusinessException("Le nom du rôle est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        role.setName(nom);
        exigerDesPermissionsConnues(role.getPermissions());

        return role.getId() == null ? creer(role) : modifier(role, nom);
    }

    /**
     * Attribue un rôle à un compte, en plus de ceux qu'il porte.
     *
     * <p>Le contrôle porte sur l'état qui en résulte : le demandeur doit détenir toutes les
     * permissions du rôle, et ne peut pas viser son propre compte.</p>
     *
     * @throws BusinessException 404 si le rôle n'existe pas, 403 si l'attribution est refusée
     */
    @Transactional
    public UserRoleAssignment attribuer(String userId, UUID roleId) {
        AppRole role = appRoleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Aucun rôle ne porte cet identifiant : " + roleId,
                        HttpStatus.NOT_FOUND));
        List<AppRole> resultat = new java.util.ArrayList<>(affectations.findByUserId(userId).stream()
                .map(UserRoleAssignment::getRole).filter(java.util.Objects::nonNull).toList());
        resultat.add(role);
        garde.exigerAttributionPermise(userId, resultat);

        return affectations.save(UserRoleAssignment.builder()
                .userId(userId)
                .role(role)
                .build());
    }

    /**
     * Les permissions du dictionnaire ({@code permissions.json}).
     *
     * <p>Publiques pour {@link KcRoleService}, qui refuse un rôle Keycloak homonyme d'une
     * permission : les rôles du jeton comptent comme permissions.</p>
     */
    public Set<String> permissionsDuDictionnaire() {
        return permissionsConnues();
    }

    private void exigerDesPermissionsConnues(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        Set<String> connues = permissionsConnues();
        List<String> inconnues = permissions.stream().filter(p -> p == null || !connues.contains(p)).toList();
        if (!inconnues.isEmpty()) {
            throw new BusinessException("Permission(s) inconnue(s) du dictionnaire : " + inconnues
                    + ". Un rôle ne porte que les permissions proposées à l'écran d'administration.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Set<String> permissionsConnues() {
        Set<String> connues = permissionsConnues;
        if (connues == null) {
            try (InputStream is = new ClassPathResource(DICTIONNAIRE).getInputStream()) {
                List<Map<String, String>> entrees = new ObjectMapper().readValue(is, new TypeReference<>() { });
                connues = entrees.stream().map(e -> e.get("value")).collect(Collectors.toUnmodifiableSet());
            } catch (IOException e) {
                throw new UncheckedIOException("Dictionnaire des permissions illisible : " + DICTIONNAIRE, e);
            }
            permissionsConnues = connues;
        }
        return connues;
    }

    /**
     * Crée un rôle, sous un nom ni réservé ni déjà pris — l'un et l'autre sans égard à la casse.
     *
     * <p>Les rôles déjà en base ne sont pas revus : leur reconnaissance reste celle d'hier. Seuls
     * les noms nouveaux sont tenus à la règle.</p>
     */
    private AppRole creer(AppRole role) {
        if (NOMS_RESERVES.contains(role.getName().toUpperCase(Locale.ROOT))) {
            throw new BusinessException(
                    "Le nom de rôle « " + role.getName() + " » est réservé : il désigne l'administration "
                            + "totale ou les appels de service, que nul ne reçoit par l'écran "
                            + "d'administration.",
                    HttpStatus.FORBIDDEN);
        }
        garde.exigerPermissionsDetenues(List.of(), role.getPermissions());
        if (!appRoleRepository.findAllByNameIgnoreCase(role.getName()).isEmpty()) {
            throw new BusinessException(
                    "Un rôle nommé « " + role.getName() + " » existe déjà, à la casse près. Les étapes "
                            + "des circuits désignent leur responsable par ce nom : deux rôles homonymes "
                            + "rendraient indécidable qui est habilité.",
                    HttpStatus.CONFLICT);
        }
        return appRoleRepository.save(role);
    }

    private AppRole modifier(AppRole role, String nom) {
        AppRole existant = appRoleRepository.findById(role.getId())
                .orElseThrow(() -> new BusinessException(
                        "Rôle introuvable : " + role.getId(), HttpStatus.NOT_FOUND));

        if (!nom.equals(existant.getName())) {
            throw new BusinessException(
                    "Le nom du rôle ne peut pas être modifié (" + existant.getName() + " → " + nom
                            + "). Les étapes des circuits, le catalogue d'étapes et les notifications "
                            + "désignent ce rôle par son nom : le changer les priverait toutes de leur "
                            + "titulaire. Créez un nouveau rôle si nécessaire, ou modifiez la "
                            + "description.",
                    HttpStatus.CONFLICT);
        }

        existant.setDescription(role.getDescription());
        List<String> permissions = role.getPermissions();
        if (permissions != null) {
            garde.exigerPermissionsDetenues(existant.getPermissions(), permissions);
            existant.setPermissions(permissions);
        }
        return appRoleRepository.save(existant);
    }
}
