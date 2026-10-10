package com.qualiapproche.userservice.service;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.common.dto.auth.KcRoleDto;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.mappers.KcRoleMapper;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import com.qualiapproche.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import jakarta.ws.rs.NotFoundException;

/**
 * Rôles du realm Keycloak, et rôles applicatifs pour leur suppression.
 *
 * <p>Les rôles du realm finissent dans le jeton ({@code realm_access.roles}), et
 * {@code PermissionChecker} les compte comme des permissions : un rôle Keycloak nommé
 * {@code MANAGE_USER}, {@code ROLE_MANAGE_USER} ou {@code SUPER_ADMIN} ouvre ce qu'ouvrirait la
 * permission du même nom, sans passer par les rôles applicatifs ni par leurs garde-fous. Aucun écran
 * ne crée ni n'attribue de rôle Keycloak — l'administration se fait par les rôles applicatifs — mais
 * les points d'entrée existent.</p>
 *
 * <p>Refuser ces seuls noms ne suffisait pas : le moteur de workflow accorde aussi le droit de
 * décider à une étape à qui porte, dans son jeton, un rôle du nom de l'habilitation exigée. Un
 * gestionnaire des rôles créait {@code RESPONSABLE_QUALITE} côté Keycloak et l'attribuait à un
 * collègue, qui décidait alors sans détenir les permissions du rôle applicatif homonyme — ce que
 * {@code /app-roles/assign} refuse. Toute écriture d'un rôle Keycloak est donc réservée au
 * SUPER_ADMIN ({@link GardeDesHabilitations#exigerSuperAdmin}) ; les noms réservés restent refusés
 * même à lui, car ils ouvriraient une permission en dehors des rôles applicatifs.</p>
 */
@Service
@RequiredArgsConstructor
public class KcRoleService {

    /** Noms qu'aucun rôle Keycloak ne peut porter, outre ceux des permissions du dictionnaire. */
    private static final Set<String> NOMS_RESERVES = Set.of("SUPER_ADMIN", "SUPERADMIN", "SERVICE_TECHNIQUE");

    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;
    private final KcRoleMapper kcRoleMapper;
    private final AppRoleRepository appRoleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final AppRoleService appRoleService;
    private final GardeDesHabilitations garde;

    @Value("${keycloak.realm}")
    private String realm;

    public Page<KcRoleDto> getAllRoles(Pageable pageable) {
        List<RoleRepresentation> allRoles = keycloak.realm(realm).roles().list();
        List<KcRoleDto> allDtos = allRoles.stream()
                .map(kcRoleMapper::toDto)
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allDtos.size());
        List<KcRoleDto> page = start >= allDtos.size() ? List.of() : allDtos.subList(start, end);
        return new PageImpl<>(page, pageable, allDtos.size());
    }

    public KcRoleDto getRoleByName(String roleName) {
        RoleRepresentation roleRepresentation = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        return kcRoleMapper.toDto(roleRepresentation);
    }

    public KcRoleDto createRole(KcRoleDto kcRoleDto) {
        if (kcRoleDto.getName() == null || kcRoleDto.getName().isBlank()) {
            throw new BusinessException("Le nom du rôle est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        garde.exigerSuperAdmin("créer un rôle Keycloak");
        exigerUnNomNonReserve(kcRoleDto.getName());
        RoleRepresentation roleRepresentation = kcRoleMapper.toEntity(kcRoleDto);
        keycloak.realm(realm).roles().create(roleRepresentation);
        RoleRepresentation createdRole = keycloak.realm(realm)
                .roles()
                .get(kcRoleDto.getName())
                .toRepresentation();
        return kcRoleMapper.toDto(createdRole);
    }

    public void updateRole(KcRoleDto kcRoleDto) {
        garde.exigerSuperAdmin("modifier un rôle Keycloak");
        RoleRepresentation roleRepresentation = kcRoleMapper.toEntity(kcRoleDto);
        keycloak.realm(realm).roles().get(kcRoleDto.getName()).update(roleRepresentation);
    }

    public void deleteRole(String roleName) {
        garde.exigerSuperAdmin("supprimer un rôle Keycloak");
        keycloak.realm(realm).roles().deleteRole(roleName);
    }

    /**
     * Supprime un rôle applicatif, et refuse de le faire quand il est encore porté.
     *
     * <p>Le contrôle des porteurs vient avant la suppression, et il vaut mieux que la contrainte
     * de la base. Sans lui, PostgreSQL refusait le retrait d'un rôle encore attribué et la
     * violation remontait en exception brute, que l'appelant traduisait en « introuvable » — un
     * rôle parfaitement existant se disait absent, sans que rien n'indique qu'il suffisait de le
     * retirer à ses porteurs.</p>
     *
     * <p>Les permissions du rôle, elles, ne retiennent rien : {@code @ElementCollection} les fait
     * disparaître avec lui.</p>
     */
    public void deleteRoleById(UUID id) {
        AppRole role = appRoleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Aucun rôle ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND));

        long porteurs = userRoleAssignmentRepository.countByRole_Id(id);
        if (porteurs > 0) {
            throw new BusinessException(
                    "Le rôle « " + role.getName() + " » est encore attribué à " + porteurs
                            + " utilisateur" + (porteurs > 1 ? "s" : "")
                            + ". Retirez-le-leur avant de le supprimer.",
                    HttpStatus.CONFLICT);
        }

        appRoleRepository.delete(role);
    }


    /**
     * Remplace les rôles Keycloak d'un compte par ceux nommés.
     *
     * <p>Réservé au SUPER_ADMIN ; refuse d'ajouter un nom réservé, et refuse à quiconque de
     * modifier ses propres rôles.</p>
     */
    public void assignRoles(String userId, List<String> newRoleNames) {
        if (newRoleNames == null) {
            throw new BusinessException("La liste des rôles est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        garde.exigerSuperAdmin("attribuer ou retirer un rôle Keycloak");
        UserResource userResource = getUserResourcebyId(userId);
        RoleScopeResource roleScopeResource = userResource.roles().realmLevel();
        List<RoleRepresentation> existingRoles = roleScopeResource.listAll();

        List<String> existingRoleNames = existingRoles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        List<RoleRepresentation> rolesToRemove = existingRoles.stream()
                .filter(role -> !newRoleNames.contains(role.getName()))
                .collect(Collectors.toList());

        List<String> nomsAjoutes = newRoleNames.stream()
                .filter(roleName -> roleName != null && !existingRoleNames.contains(roleName))
                .toList();
        if ((!rolesToRemove.isEmpty() || !nomsAjoutes.isEmpty())
                && userId.equals(com.qualiapproche.common.utils.SecurityUtils.getCurrentUserId())) {
            throw new BusinessException("Vous ne pouvez pas modifier les rôles de votre propre compte : "
                    + "demandez-le à un autre administrateur.", HttpStatus.FORBIDDEN);
        }
        nomsAjoutes.forEach(this::exigerUnNomNonReserve);

        RolesResource rolesResource = getRolesResource();
        List<RoleRepresentation> rolesToAdd = nomsAjoutes.stream()
                .map(roleName -> {
                    try {
                        return rolesResource.get(roleName).toRepresentation();
                    } catch (NotFoundException e) {
                        return null; // Le rôle n'existe pas dans Keycloak
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        if (!rolesToRemove.isEmpty()) {
            roleScopeResource.remove(rolesToRemove);
        }
        if (!rolesToAdd.isEmpty()) {
            roleScopeResource.add(rolesToAdd);
        }
    }

    public List<KcRoleDto> getRolesForUser(String userId) {
        RoleScopeResource roleScopeResource = keycloak.realm(realm).users().get(userId).roles().realmLevel();
        List<RoleRepresentation> roleRepresentations = roleScopeResource.listAll();
        return roleRepresentations.stream()
                .map(kcRoleMapper::toDto)
                .collect(Collectors.toList());
    }

    /** 403 si le nom ouvrirait, une fois dans le jeton, une permission ou l'administration totale. */
    private void exigerUnNomNonReserve(String nom) {
        if (nomReserve(nom, appRoleService.permissionsDuDictionnaire())) {
            throw new BusinessException("Le nom de rôle « " + nom + " » est réservé : il coïncide avec une "
                    + "permission ou un rôle d'administration, et ouvrirait ses droits à qui le porte. "
                    + "Attribuez plutôt un rôle applicatif.", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Le rôle Keycloak {@code nom} satisferait-il une permission ou un rôle réservé ?
     *
     * <p>Le jeton le présente comme l'autorité {@code ROLE_<NOM>}, et {@code PermissionChecker}
     * accepte une exigence {@code R} sous trois formes : {@code R}, {@code ROLE_R}, ou {@code R}
     * privée de son préfixe {@code ROLE_}. Le nom est donc réservé si {@code NOM},
     * {@code ROLE_NOM} ou {@code ROLE_ROLE_NOM} est une exigence possible — sans égard à la casse,
     * que le contrôle ignore.</p>
     */
    static boolean nomReserve(String nom, Set<String> permissions) {
        String n = nom.trim().toUpperCase(Locale.ROOT);
        Set<String> reserves = new java.util.HashSet<>(NOMS_RESERVES);
        permissions.stream().filter(java.util.Objects::nonNull)
                .map(p -> p.toUpperCase(Locale.ROOT)).forEach(reserves::add);
        return reserves.contains(n) || reserves.contains("ROLE_" + n) || reserves.contains("ROLE_ROLE_" + n);
    }

    private RolesResource getRolesResource() {
        return keycloak.realm(realm).roles();
    }

    public UserResource getUserResourcebyId(String userId) {
        return keycloak.realm(realm).users().get(userId);
    }
}
