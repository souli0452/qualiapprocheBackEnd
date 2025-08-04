package com.qualiapproche.service.kcService;

import com.qualiapproche.config.utils.KcAuthProperties;
import com.qualiapproche.dto.auth.KcRoleDto;
import com.qualiapproche.entities.mappers.KcRoleMapper;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcRoleService {

    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;
    @Value("${kc.auth.realm}")
    private String realm;
    private final KcRoleMapper kcRoleMapper;

    public List<KcRoleDto> getAllRoles() {
        List<RoleRepresentation> roleRepresentations = keycloak.realm(realm).roles().list();
        return kcRoleMapper.toDto(roleRepresentations);
    }

    public KcRoleDto getRoleByName(String roleName) {
        RoleRepresentation roleRepresentation = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        return kcRoleMapper.toDto(roleRepresentation);
    }

    public KcRoleDto createRole(KcRoleDto kcRoleDto) {
        RoleRepresentation roleRepresentation = kcRoleMapper.toEntity(kcRoleDto);
        keycloak.realm(kcAuthProperties.getRealm()).roles().create(roleRepresentation);
        RoleRepresentation createdRole = keycloak.realm(kcAuthProperties.getRealm())
                .roles()
                .get(kcRoleDto.getName())
                .toRepresentation();
        return kcRoleMapper.toDto(createdRole);
    }

    public RoleRepresentation mapRoleRep(KcRoleDto role) {
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(role.getName());
        return roleRep;
    }

    public void updateRole(KcRoleDto kcRoleDto) {
        RoleRepresentation roleRepresentation = kcRoleMapper.toEntity(kcRoleDto);
        keycloak.realm(realm).roles().get(kcRoleDto.getName()).update(roleRepresentation);
    }

    public void deleteRole(String roleName) {
        keycloak.realm(realm).roles().deleteRole(roleName);
    }

    private KcRoleDto mapToRole(Response response) {
        KcRoleDto kcRoleDto = null;
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            String location = response.getHeaderString("Location");
            String roleId = location != null ? location.substring(location.lastIndexOf('/') + 1) : null;

            if (roleId == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue lors de la création du rôle.");
            }

            RoleRepresentation createdRole = keycloak.realm(kcAuthProperties.getRealm())
                    .roles()
                    .get(roleId)
                    .toRepresentation();

            kcRoleDto = kcRoleMapper.toDto(createdRole);
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue lors de la création du rôle.");
        }

        response.close();

        return kcRoleDto;
    }

    public void assignRoles(String userId, List<String> newRoleNames) {
        try {
            UserResource userResource = getUserResourcebyId(userId);
            if (userResource == null) {
                throw new IllegalArgumentException("Utilisateur non trouvé pour l'ID: " + userId);
            }
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
                    .map(roleName -> rolesResource.get(roleName).toRepresentation())
                    .collect(Collectors.toList());
            if (!rolesToRemove.isEmpty()) {
                roleScopeResource.remove(rolesToRemove);
            }
            if (!rolesToAdd.isEmpty()) {
                roleScopeResource.add(rolesToAdd);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Une erreur est survenue lors de l'assignation des rôles", e);
        }
    }


    public List<KcRoleDto> getRolesForUser(String userId) {
        try {
            RoleScopeResource roleScopeResource = keycloak.realm(realm).users().get(userId).roles().realmLevel();
            List<RoleRepresentation> roleRepresentations = roleScopeResource.listAll();
            return roleRepresentations.stream()
                    .map(role -> kcRoleMapper.toDto(role))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des rôles pour l'utilisateur avec ID: " + userId, e);
            throw new RuntimeException("Erreur lors de la récupération des rôles de l'utilisateur", e);
        }
    }

    private RolesResource getRolesResource(){
        return  keycloak.realm(realm).roles();
    }

    public UserResource getUserResourcebyId(String userId) {
        return keycloak.realm(realm).users().get(userId);
    }

}
