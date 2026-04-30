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

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KcUserService {
    private final KcTokenService kcTokenService;
    private final KcUserMapper kcUserMapper;
    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;
    private final SendMailService sendMailService;
    private final KcRoleService kcRoleService;

    @Value("${frontend.url}")
    private String frontendUrl;

    private static String getAttributeValue(Map<String, List<String>> attributes, String key) {
        return (attributes != null && attributes.containsKey(key) && attributes.get(key) != null && !attributes.get(key).isEmpty())
                ? attributes.get(key).get(0)
                : null;
    }

    public ResponseEntity<Object> login(KcLoginRequestDto loginRequest, HttpServletResponse response) {
        String realm = kcAuthProperties.getRealm();
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        List<UserRepresentation> userRepresentations = usersResource.search(loginRequest.getUsername(), 0, 1);

        if (userRepresentations.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        UserRepresentation user = userRepresentations.get(0);
        UserStatusDto userStatus = checkCredentialsByUsername(loginRequest.getUsername());

        KcTokenDto kcTokenDto = kcTokenService.getAccessToken(loginRequest);

        response.addHeader(KcConstants.ACCESS_TOKEN, kcTokenDto.getAccessToken());
        response.addHeader(KcConstants.REFRESH_TOKEN, kcTokenDto.getRefreshToken());
        response.addHeader(KcConstants.EXPIRES_IN, String.valueOf(kcTokenDto.getExpiresIn()));

        List<KcRoleDto> userRoles = kcRoleService.getRolesForUser(user.getId());

        Map<String, Object> responseData = getResponseData(user, kcTokenDto, userRoles);

        return ResponseEntity.ok().body(KcResponseDto.builder()
                .status("SUCCESS")
                .data(responseData)
                .build());
    }

    private static Map<String, Object> getResponseData(UserRepresentation user, KcTokenDto kcTokenDto, List<KcRoleDto> userRoles) {
        Map<String, String> userData = new HashMap<>();
        Map<String, List<String>> attributes = user.getAttributes();

        userData.put("userId", user.getId());
        userData.put("email", user.getEmail());
        userData.put("username", user.getUsername());
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());

        List<String> roles = userRoles.stream()
                .map(KcRoleDto::getName)
                .toList();
        userData.put("roles", roles.toString());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("access_token", kcTokenDto.getAccessToken());
        responseData.put("refresh_token", kcTokenDto.getRefreshToken());
        responseData.put("expires_in", kcTokenDto.getExpiresIn());
        responseData.put("refresh_expires_in", kcTokenDto.getRefreshExpiresIn());
        responseData.put("token_type", kcTokenDto.getTokenType());
        responseData.put("scope", kcTokenDto.getScope());
        responseData.put("user", userData);
        responseData.put("fonction", getAttributeValue(attributes, "fonction"));

        return responseData;
    }

    public UserStatusDto checkCredentialsByUsername(String username) {
        String realm = kcAuthProperties.getRealm();
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .search(username, true);

        if (users.isEmpty()) {
            return new UserStatusDto(false, false, false);
        }

        UserRepresentation rep = users.get(0);

        boolean isEmailVerified = rep.isEmailVerified() != null && rep.isEmailVerified();
        boolean enabled = rep.isEnabled();
        boolean temporaryPwd = false;

        List<String> requiredActions = rep.getRequiredActions();
        if (requiredActions != null && requiredActions.contains("UPDATE_PASSWORD")) {
            temporaryPwd = true;
        }

        return new UserStatusDto(isEmailVerified, enabled, temporaryPwd);
    }

    public ResponseEntity<Object> refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        KcTokenDto kcTokenDto = kcTokenService.getRefreshToken(refreshToken);

        response.addHeader(KcConstants.ACCESS_TOKEN, kcTokenDto.getAccessToken());
        response.addHeader(KcConstants.EXPIRES_IN, String.valueOf(kcTokenDto.getExpiresIn()));

        return ResponseEntity.ok().body(KcResponseDto.builder()
                .status("SUCCESS")
                .data(kcTokenDto)
                .build());
    }

    public List<KcUserDto> getAllUsers() {
        List<UserRepresentation> users = keycloak.realm(kcAuthProperties.getRealm()).users().list();
        return users.stream()
                .map(user -> {
                    KcUserDto kcUserDto = kcUserMapper.toDto(user);
                    Map<String, List<String>> attributes = user.getAttributes();
                    kcUserDto.setPhoneNumber(getAttributeValue(attributes, "phoneNumber"));
                    kcUserDto.setStructure(getAttributeValue(attributes, "structure"));
                    kcUserDto.setFonction(getAttributeValue(attributes, "fonction"));
                    
                    // Récupérer les rôles pour chaque utilisateur
                    List<KcRoleDto> userRoles = kcRoleService.getRolesForUser(user.getId());
                    kcUserDto.setRoles(userRoles.stream().map(KcRoleDto::getName).collect(Collectors.toList()));
                    
                    return kcUserDto;
                })
                .collect(Collectors.toList());
    }

    public List<KcUserDto> getUsersByStructure(String structure) {
        List<UserRepresentation> users = keycloak.realm(kcAuthProperties.getRealm()).users().list();
        return users.stream()
                .map(user -> {
                    KcUserDto kcUserDto = kcUserMapper.toDto(user);
                    Map<String, List<String>> attributes = user.getAttributes();
                    kcUserDto.setPhoneNumber(getAttributeValue(attributes, "phoneNumber"));
                    kcUserDto.setStructure(getAttributeValue(attributes, "structure"));
                    kcUserDto.setFonction(getAttributeValue(attributes, "fonction"));

                    // Récupérer les rôles pour chaque utilisateur
                    List<KcRoleDto> userRoles = kcRoleService.getRolesForUser(user.getId());
                    kcUserDto.setRoles(userRoles.stream().map(KcRoleDto::getName).collect(Collectors.toList()));

                    return kcUserDto;
                })
                .filter(kcUserDto -> kcUserDto.getStructure() != null && kcUserDto.getStructure().equals(structure))
                .collect(Collectors.toList());
    }

    public KcUserDto getUserById(String userId) {
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .toRepresentation();

        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }
        KcUserDto kcUserDto = kcUserMapper.toDto(user);
        if (user.getAttributes() != null && user.getAttributes().containsKey("phoneNumber")) {
            kcUserDto.setPhoneNumber(user.getAttributes().get("phoneNumber").get(0));
        }
        if (user.getAttributes() != null && user.getAttributes().containsKey("structure")) {
            kcUserDto.setStructure(user.getAttributes().get("structure").get(0));
        }
        return kcUserDto;
    }

    public List<KcUserDto> getAllUsersByStructure(String structureId) {
        List<UserRepresentation> users;
        if (structureId != null) {
            GroupRepresentation group = keycloak.realm(kcAuthProperties.getRealm())
                    .groups()
                    .groups()
                    .stream()
                    .filter(g -> structureId.equals(g.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Group not found: " + structureId));

            users = keycloak.realm(kcAuthProperties.getRealm())
                    .groups()
                    .group(group.getId())
                    .members();
        } else {
            users = keycloak.realm(kcAuthProperties.getRealm()).users().list();
        }

        return users.stream()
                .map(user -> {
                    KcUserDto kcUserDto = kcUserMapper.toDto(user);
                    Map<String, List<String>> attributes = user.getAttributes();
                    kcUserDto.setStructure(getAttributeValue(attributes, "structure"));
                    return kcUserDto;
                })
                .collect(Collectors.toList());
    }

    public KcUserDto createUser(KcUserDto kcUserDto) {
        UserRepresentation user = kcUserMapper.toEntity(kcUserDto);
        String password = generateRandomPassword(8);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(Boolean.TRUE);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        user.setCredentials(Collections.singletonList(credential));
        Response response = keycloak.realm(kcAuthProperties.getRealm()).users().create(user);
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            String location = response.getHeaderString("Location");
            String userId = location != null ? location.substring(location.lastIndexOf('/') + 1) : null;

            String recipientEmail = kcUserDto.getEmail();
            String firstName = kcUserDto.getFirstName();
            String lastName = kcUserDto.getLastName();
            String token = kcTokenService.generateToken(userId);

            String verificationUrl = frontendUrl + "/verify-email?token=" + token + "&userId=" + userId;
            sendMailService.sendVerificationEmail(recipientEmail, firstName, lastName, password, verificationUrl);

            // Assigner les rôles à l'utilisateur
            if (kcUserDto.getRoles() != null && !kcUserDto.getRoles().isEmpty()) {
                kcRoleService.assignRoles(userId, kcUserDto.getRoles());
            }

            return getUserById(userId);
        } else {
            throw new RuntimeException("Erreur lors de la création de l'utilisateur : " + response.getStatus());
        }
    }

    public void updateUser(final KcUserDto kcUserDto) {
        String userId = kcUserDto.getId();
        UserRepresentation createdUser = keycloak.realm(kcAuthProperties.getRealm())
                .users().get(userId).toRepresentation();

        if (createdUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le compte utilisateur n'existe pas.");
        }

        keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).update(kcUserMapper.toEntity(kcUserDto));

        // Mise à jour des rôles
        if (kcUserDto.getRoles() != null) {
            kcRoleService.assignRoles(userId, kcUserDto.getRoles());
        }
    }

    public void updatePassword(String userId, String oldPassword) {
        String newPassword = generateRandomPassword(8);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(true);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);

        keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .resetPassword(credential);

        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .toRepresentation();
        String email = user.getEmail();
        String url = frontendUrl + "/login";
        sendMailService.sendResetPasswordEmail(email, newPassword, url);
    }

    public void changeUserStatus(final String userId, final boolean enabled) {
        UserRepresentation userRepresentation = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .toRepresentation();
        if (userRepresentation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le compte utilisateur n'existe pas.");
        }

        userRepresentation.setEnabled(enabled);
        keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .update(userRepresentation);
    }

    public void emailVerification(final String userId, final String token) {
        String tokenUserId;
        try {
            tokenUserId = kcTokenService.validateToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide ou expiré.");
        }

        if (tokenUserId == null || !tokenUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide pour l'utilisateur.");
        }

        UserRepresentation userRepresentation = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .toRepresentation();
        if (userRepresentation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le compte utilisateur n'existe pas.");
        }

        userRepresentation.setEmailVerified(true);
        keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .update(userRepresentation);
    }

    public void reinitializePwd(final String userId, final String newPassword, final String token) {
        String tokenUserId;
        try {
            tokenUserId = kcTokenService.validateToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide ou expiré.");
        }

        if (tokenUserId == null || !tokenUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide pour l'utilisateur.");
        }

        UserRepresentation userRepresentation = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .toRepresentation();

        if (userRepresentation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le compte utilisateur n'existe pas.");
        }
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(newPassword);
        cred.setTemporary(false);
        
        keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .resetPassword(cred);
    }

    public ResponseEntity<Object> updatePassword(final String username, final String oldPassword, final String newPassword) {
        UserRepresentation userRepresentation = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .search(username)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        String userId = userRepresentation.getId();

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        try {
            keycloak.realm(kcAuthProperties.getRealm())
                    .users()
                    .get(userId)
                    .resetPassword(credential);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la mise à jour du mot de passe.");
        }

        KcTokenDto tokenDto = kcTokenService.getAccessToken(new KcLoginRequestDto(username, newPassword, null));

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("access_token", tokenDto.getAccessToken());
        responseData.put("refresh_token", tokenDto.getRefreshToken());

        KcResponseDto responseDto = KcResponseDto.builder()
                .status("SUCCESS")
                .data(responseData)
                .build();

        return ResponseEntity.ok().body(responseDto);
    }

    public boolean isEmailVerified(final String userId) {
        UserRepresentation userRepresentation = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .get(userId)
                .toRepresentation();
        if (userRepresentation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le compte utilisateur n'existe pas.");
        }
        return userRepresentation.isEmailVerified();
    }

    public UserRepresentation findUserByEmail(String email) {
        List<UserRepresentation> users = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .search(email, null, null, null, 0, 1);
        return users.isEmpty() ? null : users.get(0);
    }

    public void initiatePasswordReset(String email) {
        UserRepresentation user = findUserByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé.");
        }

        String userId = user.getId();
        String token = kcTokenService.generateToken(userId);
        String appUrl = frontendUrl + "/reset-password?token=" + token + "&userId=" + userId;
        sendMailService.sendReinitializePasswordEmail(email, appUrl);
    }

    public String generateRandomPassword(int length) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }
}
