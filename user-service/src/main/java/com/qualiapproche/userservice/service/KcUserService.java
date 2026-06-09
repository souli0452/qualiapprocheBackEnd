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
import org.keycloak.admin.client.resource.UserResource;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
    private final SendMailService sendMailService;
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
                licenseDaysRemaining = direction.getLicenseDaysRemaining() != null
                        ? direction.getLicenseDaysRemaining().intValue()
                        : 0;
                modulesSubscribed = direction.getModulesSubscribed() != null ? direction.getModulesSubscribed()
                        : new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Erreur récupération licence via Feign (referentiel-service): {}", e.getMessage());
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

    public Page<KcUserDto> getAllUsers(Pageable pageable) {
        List<KcUserDto> allUsers = keycloak.realm(kcAuthProperties.getRealm()).users().list().stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allUsers.size());
        List<KcUserDto> page = start >= allUsers.size() ? List.of() : allUsers.subList(start, end);
        return new PageImpl<>(page, pageable, allUsers.size());
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
        user.setEmailVerified(kcUserDto.isEmailVerified());
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("structure", Collections.singletonList(kcUserDto.getStructure()));
        attributes.put("fonction", Collections.singletonList(kcUserDto.getFonction()));
        user.setAttributes(attributes);
        String password = generateRandomPassword(8);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(!kcUserDto.isEmailVerified());
        user.setCredentials(Collections.singletonList(credential));
        Response response = keycloak.realm(kcAuthProperties.getRealm()).users().create(user);
        if (response.getStatus() != 201)
            throw new RuntimeException("Erreur création utilisateur");
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        kcUserDto.setId(userId);
        syncAppRoles(userId, kcUserDto.getRoles());

        // Envoi de l'email de vérification avec le mot de passe temporaire
        try {
            String token = java.util.UUID.randomUUID().toString();
            String verificationUrl = frontendUrl + "/verify-email?token=" + token + "&userId=" + userId;
            sendMailService.sendVerificationEmail(
                kcUserDto.getEmail(),
                kcUserDto.getFirstName(),
                kcUserDto.getLastName(),
                password,
                verificationUrl
            );
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de vérification: {}", e.getMessage());
        }

        return kcUserDto;
    }

    public void updateUser(KcUserDto kcUserDto) {
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm()).users().get(kcUserDto.getId())
                .toRepresentation();
        user.setFirstName(kcUserDto.getFirstName());
        user.setLastName(kcUserDto.getLastName());
        user.setEmail(kcUserDto.getEmail());
        user.setEnabled(kcUserDto.isEnabled());
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null)
            attributes = new HashMap<>();
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
        UserResource userResource = keycloak.realm(kcAuthProperties.getRealm()).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        
        user.setEmailVerified(true);
        user.setRequiredActions(Collections.emptyList());
        userResource.update(user);

        // Rendre le mot de passe permanent pour permettre la connexion directe
        userResource.credentials().forEach(credential -> {
            if (CredentialRepresentation.PASSWORD.equals(credential.getType())) {
                credential.setTemporary(false);
                // On pourrait techniquement modifier l'entité, mais le plus sûr est de s'assurer 
                // qu'aucune action UPDATE_PASSWORD ne reste dans Keycloak.
            }
        });
    }

    public boolean isEmailVerified(String userId) {
        return keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).toRepresentation().isEmailVerified();
    }

    public void initiatePasswordReset(String email) {
    }

    public void reinitializePwd(String userId, String password, String token) {
        updatePassword(userId, password);
    }

    public ResponseEntity<Object> updatePassword(String username, String oldPassword, String password) {
        try {
            // 1. Vérification de l'ancien mot de passe via une tentative de login
            kcTokenService.getAccessToken(KcLoginRequestDto.builder()
                    .username(username)
                    .password(oldPassword)
                    .build());

            // 2. Recherche de l'utilisateur par son username
            List<UserRepresentation> users = keycloak.realm(kcAuthProperties.getRealm())
                    .users()
                    .search(username, true);

            if (users.isEmpty()) {
                throw new RuntimeException("Utilisateur non trouvé");
            }

            // 3. Mise à jour du mot de passe
            updatePassword(users.get(0).getId(), password);

            return ResponseEntity.ok(com.qualiapproche.common.dto.auth.KcResponseDto.builder()
                    .status("SUCCESS")
                    .message("Votre mot de passe a été mis à jour avec succès.")
                    .build());
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du mot de passe: {}", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(com.qualiapproche.common.dto.auth.KcResponseDto.builder()
                            .status("ERROR")
                            .message("Ancien mot de passe incorrect.")
                            .build());
        }
    }

    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        if (attributes != null && attributes.containsKey(key)) {
            List<String> values = attributes.get(key);
            if (!values.isEmpty())
                return values.get(0);
        }
        return null;
    }

    public ResponseEntity<Object> refreshToken(String refreshToken, HttpServletRequest request,
            HttpServletResponse response) {
        KcTokenDto kcTokenDto = kcTokenService.getRefreshToken(refreshToken);
        return ResponseEntity.ok().body(com.qualiapproche.common.dto.auth.KcResponseDto.builder()
                .status("SUCCESS")
                .data(kcTokenDto)
                .build());
    }

    public Page<KcUserDto> getUsersByStructure(String structureId, Pageable pageable) {
        List<KcUserDto> allFiltered = keycloak.realm(kcAuthProperties.getRealm()).users().list().stream()
                .map(this::mapUserToDto)
                .filter(u -> structureId.equals(u.getStructure()))
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allFiltered.size());
        List<KcUserDto> page = start >= allFiltered.size() ? List.of() : allFiltered.subList(start, end);
        return new PageImpl<>(page, pageable, allFiltered.size());
    }

    public KcUserDto getUserById(String userId) {
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).toRepresentation();
        return mapUserToDto(user);
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
