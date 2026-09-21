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
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import jakarta.ws.rs.NotFoundException;

@Service
@RequiredArgsConstructor
public class KcRoleService {

    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;
    private final KcRoleMapper kcRoleMapper;
    private final AppRoleRepository appRoleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

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
        RoleRepresentation roleRepresentation = kcRoleMapper.toEntity(kcRoleDto);
        keycloak.realm(realm).roles().create(roleRepresentation);
        RoleRepresentation createdRole = keycloak.realm(realm)
                .roles()
                .get(kcRoleDto.getName())
                .toRepresentation();
        return kcRoleMapper.toDto(createdRole);
    }

    public void updateRole(KcRoleDto kcRoleDto) {
        RoleRepresentation roleRepresentation = kcRoleMapper.toEntity(kcRoleDto);
        keycloak.realm(realm).roles().get(kcRoleDto.getName()).update(roleRepresentation);
    }

    public void deleteRole(String roleName) {
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


    public void assignRoles(String userId, List<String> newRoleNames) {
        UserResource userResource = getUserResourcebyId(userId);
        RoleScopeResource roleScopeResource = userResource.roles().realmLevel();
        List<RoleRepresentation> existingRoles = roleScopeResource.listAll();

        List<String> existingRoleNames = existingRoles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        List<RoleRepresentation> rolesToRemove = existingRoles.stream()
                .filter(role -> !newRoleNames.contains(role.getName()))
                .collect(Collectors.toList());

        RolesResource rolesResource = getRolesResource();
        List<RoleRepresentation> rolesToAdd = newRoleNames.stream()
                .filter(roleName -> !existingRoleNames.contains(roleName))
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

    private RolesResource getRolesResource() {
        return keycloak.realm(realm).roles();
    }

    public UserResource getUserResourcebyId(String userId) {
        return keycloak.realm(realm).users().get(userId);
    }
}
