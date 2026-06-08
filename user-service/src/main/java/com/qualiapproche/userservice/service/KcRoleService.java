package com.qualiapproche.userservice.service;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.common.dto.auth.KcRoleDto;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.mappers.KcRoleMapper;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class KcRoleService {

    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;
    private final KcRoleMapper kcRoleMapper;
    private final AppRoleRepository appRoleRepository;

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

    public void deleteRoleById(UUID id) {
        AppRole role = appRoleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
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
                    } catch (jakarta.ws.rs.NotFoundException e) {
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

    private RolesResource getRolesResource(){
        return keycloak.realm(realm).roles();
    }

    public UserResource getUserResourcebyId(String userId) {
        return keycloak.realm(realm).users().get(userId);
    }
}
