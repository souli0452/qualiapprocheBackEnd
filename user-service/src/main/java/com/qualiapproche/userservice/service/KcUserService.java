package com.qualiapproche.userservice.service;

import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.userservice.config.auth.KcConstants;

import com.qualiapproche.common.dto.UserStatusDto;
import com.qualiapproche.common.dto.auth.*;

import com.qualiapproche.userservice.entities.mappers.KcUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KcUserService {
    private final KcTokenService kcTokenService;
    private final KcUserMapper kcUserMapper;
    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;
    private final com.qualiapproche.userservice.repository.AppRoleRepository appRoleRepository;
    private final com.qualiapproche.userservice.repository.UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final com.qualiapproche.userservice.client.StructureClient structureClient;

    @Value("${frontend.url}")
    private String frontendUrl;

    public Map<String, Object> login(KcLoginRequestDto kcLoginRequest) {
        KcTokenDto kcTokenDto = kcTokenService.getAccessToken(kcLoginRequest);

        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .search(kcLoginRequest.getUsername(), true)
                .get(0);

        Map<String, List<String>> attributes = user.getAttributes();
        String structureId = getAttributeValue(attributes, "structure");

        // RÉCUPÉRATION DE LA LICENCE (Retour à la logique initiale via getDirection)
        boolean licenseActive = false;
        int licenseDaysRemaining = 0;
        List<String> modulesSubscribed = new java.util.ArrayList<>();

        try {
            com.qualiapproche.common.dto.StructureDto direction = structureClient.getDirection();
            if (direction != null) {
                licenseActive = direction.getLicenceActive() != null && direction.getLicenceActive();
                licenseDaysRemaining = direction.getLicenseDaysRemaining() != null ? direction.getLicenseDaysRemaining().intValue() : 0;
                modulesSubscribed = direction.getModulesSubscribed() != null ? direction.getModulesSubscribed() : new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Erreur récupération licence globale: {}", e.getMessage());
        }

        // Rôles Applicatifs (Base de données locale uniquement)
        List<String> appRoles = userRoleAssignmentRepository.findByUserId(user.getId()).stream()
                .map(assignment -> assignment.getRole().getName())
                .collect(Collectors.toList());

        boolean isSuperAdmin = appRoles.stream()
                .anyMatch(r -> r.equalsIgnoreCase("SUPER_ADMIN") || r.equalsIgnoreCase("SUPERADMIN"));

        if (isSuperAdmin && !appRoles.contains("SUPER_ADMIN")) {
            appRoles.add("SUPER_ADMIN");
        }

        // Permissions
        List<String> permissions;
        if (isSuperAdmin) {
            permissions = appRoleRepository.findAll().stream()
                    .flatMap(role -> role.getPermissions().stream()).distinct().collect(Collectors.toList());
        } else {
            permissions = appRoleRepository.findAll().stream()
                    .filter(role -> appRoles.contains(role.getName()))
                    .flatMap(role -> role.getPermissions().stream()).distinct().collect(Collectors.toList());
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        userMap.put("structure", structureId);
        userMap.put("fonction", getAttributeValue(attributes, "fonction"));
        userMap.put("roles", appRoles);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("access_token", kcTokenDto.getAccessToken());
        responseData.put("refresh_token", kcTokenDto.getRefreshToken());
        responseData.put("expires_in", kcTokenDto.getExpiresIn());
        responseData.put("refresh_expires_in", kcTokenDto.getRefreshExpiresIn());
        responseData.put("token_type", kcTokenDto.getTokenType());
        responseData.put("scope", kcTokenDto.getScope());
        responseData.put("user", userMap);
        responseData.put("appRoles", appRoles);
        responseData.put("permissions", permissions);
        responseData.put("licenseActive", licenseActive);
        responseData.put("licenseDaysRemaining", licenseDaysRemaining);
        responseData.put("modulesSubscribed", modulesSubscribed);
        responseData.put("fonction", getAttributeValue(attributes, "fonction"));

        return responseData;
    }

    public List<KcUserDto> getAllUsers() {
        return keycloak.realm(kcAuthProperties.getRealm()).users().list().stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }

    private KcUserDto mapUserToDto(UserRepresentation user) {
        KcUserDto dto = kcUserMapper.toDto(user);
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes != null) {
            dto.setStructure(getAttributeValue(attributes, "structure"));
            dto.setFonction(getAttributeValue(attributes, "fonction"));
        }
        List<String> appRoles = userRoleAssignmentRepository.findByUserId(user.getId()).stream()
                .map(a -> a.getRole().getName())
                .collect(Collectors.toList());
        dto.setRoles(appRoles);
        return dto;
    }

    public KcUserDto createUser(KcUserDto kcUserDto) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(kcUserDto.getUsername());
        user.setEmail(kcUserDto.getEmail());
        user.setFirstName(kcUserDto.getFirstName());
        user.setLastName(kcUserDto.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(false);
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("structure", Collections.singletonList(kcUserDto.getStructure()));
        attributes.put("fonction", Collections.singletonList(kcUserDto.getFonction()));
        user.setAttributes(attributes);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("12345678");
        credential.setTemporary(true);
        user.setCredentials(Collections.singletonList(credential));
        Response response = keycloak.realm(kcAuthProperties.getRealm()).users().create(user);
        if (response.getStatus() != 201) throw new RuntimeException("Erreur création utilisateur");
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        kcUserDto.setId(userId);
        syncAppRoles(userId, kcUserDto.getRoles());
        return kcUserDto;
    }

    public void updateUser(KcUserDto kcUserDto) {
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm()).users().get(kcUserDto.getId()).toRepresentation();
        user.setFirstName(kcUserDto.getFirstName());
        user.setLastName(kcUserDto.getLastName());
        user.setEmail(kcUserDto.getEmail());
        user.setEnabled(kcUserDto.isEnabled());
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) attributes = new HashMap<>();
        attributes.put("structure", Collections.singletonList(kcUserDto.getStructure()));
        attributes.put("fonction", Collections.singletonList(kcUserDto.getFonction()));
        user.setAttributes(attributes);
        keycloak.realm(kcAuthProperties.getRealm()).users().get(kcUserDto.getId()).update(user);
        syncAppRoles(kcUserDto.getId(), kcUserDto.getRoles());
    }

    private void syncAppRoles(String userId, List<String> roles) {
        userRoleAssignmentRepository.findByUserId(userId).forEach(userRoleAssignmentRepository::delete);
        if (roles != null) {
            roles.forEach(roleName -> {
                appRoleRepository.findByName(roleName).ifPresent(role -> {
                    com.qualiapproche.userservice.entities.UserRoleAssignment assignment = new com.qualiapproche.userservice.entities.UserRoleAssignment();
                    assignment.setUserId(userId);
                    assignment.setRole(role);
                    userRoleAssignmentRepository.save(assignment);
                });
            });
        }
    }

    public void updatePassword(String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).resetPassword(credential);
    }

    public void changeUserStatus(String userId, boolean enabled) {
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).toRepresentation();
        user.setEnabled(enabled);
        keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).update(user);
    }

    public void emailVerification(String userId, String token) {
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).toRepresentation();
        user.setEmailVerified(true);
        keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).update(user);
    }

    public boolean isEmailVerified(String userId) {
        return keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).toRepresentation().isEmailVerified();
    }

    public void initiatePasswordReset(String email) {}
    public void reinitializePwd(String userId, String password, String token) { updatePassword(userId, password); }
    public ResponseEntity<Object> updatePassword(String username, String oldPassword, String password) { return ResponseEntity.ok().build(); }

    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        if (attributes != null && attributes.containsKey(key)) {
            List<String> values = attributes.get(key);
            if (!values.isEmpty()) return values.get(0);
        }
        return null;
    }

    public ResponseEntity<Object> refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        KcTokenDto kcTokenDto = kcTokenService.getRefreshToken(refreshToken);
        return ResponseEntity.ok().body(com.qualiapproche.common.dto.auth.KcResponseDto.builder()
                .status("SUCCESS")
                .data(kcTokenDto)
                .build());
    }

    public List<KcUserDto> getUsersByStructure(String structureId) {
        return getAllUsers().stream().filter(u -> structureId.equals(u.getStructure())).collect(Collectors.toList());
    }

    public KcUserDto getUserById(String userId) {
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).toRepresentation();
        return mapUserToDto(user);
    }
}
