package com.qualiapproche.userservice.service;

import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.userservice.entities.JetonDeCompte;
import com.qualiapproche.userservice.config.auth.CookieUtils;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;

import com.qualiapproche.common.dto.auth.KcLoginRequestDto;
import com.qualiapproche.common.dto.auth.KcTokenDto;
import com.qualiapproche.common.dto.auth.KcUserDto;

import com.qualiapproche.userservice.entities.UserRoleAssignment;
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
import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.dto.auth.KcResponseDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.config.WebClientConfig.ReponseKeycloakEnErreur;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.common.utils.PermissionsPortee;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.userservice.client.StructureClient;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class KcUserService {
    private final KcTokenService kcTokenService;
    private final KcUserMapper kcUserMapper;
    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;
    private final AppRoleRepository appRoleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final StructureClient structureClient;
    private final SendMailService sendMailService;
    private final PlafondDUtilisateurs plafondDUtilisateurs;
    private final ProfilPersonnel profilPersonnel;
    private final JetonsDeCompte jetons;
    private final GardeDesHabilitations garde;

    /**
     * Préfixe que Keycloak donne aux comptes de service ({@code client_credentials}).
     * {@code PermissionChecker.appelDeService} s'y fie pour reconnaître un appel entre services :
     * un utilisateur créé sous ce nom passerait pour un service.
     */
    static final String PREFIXE_COMPTE_DE_SERVICE = "service-account-";
    @Value("${frontend.url}")
    private String frontendUrl;

    public Map<String, Object> login(KcLoginRequestDto kcLoginRequest, HttpServletResponse response) {
        KcTokenDto kcTokenDto = jetonsPourIdentifiants(kcLoginRequest);

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
            StructureDto direction = structureClient.getDirection();
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

        List<String> permissions = computePermissions(appRoles, isSuperAdmin);

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
        // Les tokens NE SONT PLUS dans le body — ils sont dans les cookies HTTP-Only
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

        // Pose des cookies HTTP-Only sécurisés
        CookieUtils.addAccessTokenCookie(response, kcTokenDto.getAccessToken(), false);
        CookieUtils.addRefreshTokenCookie(response, kcTokenDto.getRefreshToken(), false);

        return responseData;
    }

    /**
     * Échange les identifiants contre des jetons Keycloak.
     *
     * <p>Des identifiants refusés remontaient en 500 « erreur interne » : indiscernables d'une
     * panne, et signalés comme telle par les outils d'audit. Tout refus de Keycloak — mot de passe
     * faux, compte inconnu, désactivé ou non finalisé — devient un même 401, au même message : dire
     * lequel permettrait de sonder l'annuaire. Keycloak injoignable ou en erreur reste une erreur du
     * serveur.</p>
     */
    private KcTokenDto jetonsPourIdentifiants(KcLoginRequestDto identifiants) {
        try {
            return kcTokenService.getAccessToken(identifiants);
        } catch (ReponseKeycloakEnErreur e) {
            if (e.refusDeLAppelant()) {
                throw new BusinessException("Identifiant ou mot de passe incorrect.", HttpStatus.UNAUTHORIZED);
            }
            throw e;
        }
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
        exigerUnNomDUtilisateur(kcUserDto.getUsername());
        // Les rôles demandés sont contrôlés avant que le compte n'existe : refusés après coup, ils
        // laisseraient dans Keycloak un compte sans rôle que personne n'a voulu.
        garde.exigerAttributionPermise(null, rolesNommes(kcUserDto.getRoles()));
        // Avant tout le reste : la licence borne le nombre de comptes actifs, et le dépassement
        // se dit à l'utilisateur plutôt qu'après coup. Créer le compte puis le refuser laisserait
        // un compte à demi formé dans Keycloak.
        plafondDUtilisateurs.verifierAvantCreation();

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
        if (response.getStatus() == HttpStatus.CONFLICT.value()) {
            // 409 et non 500 : referentiel-service, qui provisionne l'administrateur à chaque
            // démarrage, reconnaît à ce statut un compte déjà créé.
            throw new BusinessException("Un compte porte déjà ce nom d'utilisateur ou cette adresse.",
                    HttpStatus.CONFLICT);
        }
        if (response.getStatus() != 201) {
            throw new RuntimeException("Erreur création utilisateur");
        }
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        kcUserDto.setId(userId);
        syncAppRoles(userId, kcUserDto.getRoles());

        // Envoi de l'email de vérification avec le mot de passe temporaire
        try {
            // Le jeton était tiré au hasard puis oublié : la vérification ne le lisait pas.
            String token = jetons.emettre(userId, JetonDeCompte.Usage.VERIFICATION_COURRIEL,
                    JetonsDeCompte.DUREE_VERIFICATION);
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

    /**
     * Modifie un compte depuis l'administration : identité, adresse, activation, structure, rôles.
     *
     * <p>Le demandeur doit détenir au moins tout ce que détient le compte : changer l'adresse d'un
     * administrateur puis demander un lien de réinitialisation suffisait à prendre son compte.</p>
     */
    public void updateUser(KcUserDto kcUserDto) {
        garde.exigerAscendantSur(kcUserDto.getId(), "modifier sa fiche");
        garde.exigerAttributionPermise(kcUserDto.getId(), rolesNommes(kcUserDto.getRoles()));
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm()).users().get(kcUserDto.getId())
                .toRepresentation();
        user.setFirstName(kcUserDto.getFirstName());
        user.setLastName(kcUserDto.getLastName());
        user.setEmail(kcUserDto.getEmail());
        user.setEnabled(kcUserDto.isEnabled());
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
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
                    UserRoleAssignment assignment = new UserRoleAssignment();
                    assignment.setUserId(userId);
                    assignment.setRole(role);
                    userRoleAssignmentRepository.save(assignment);
                });
            });
        }
    }

    /** Rôles applicatifs désignés par leur nom ; un nom inconnu est ignoré, comme à l'enregistrement. */
    private List<AppRole> rolesNommes(List<String> noms) {
        if (noms == null) {
            return List.of();
        }
        return noms.stream()
                .map(appRoleRepository::findByName)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toList());
    }

    /**
     * Refuse un nom de connexion vide ou réservé aux comptes de service.
     *
     * <p>{@code PermissionChecker.appelDeService} reconnaît un service au préfixe
     * {@code service-account-} de son nom : un utilisateur ainsi nommé se verrait ouvrir les points
     * d'entrée réservés au dialogue entre services.</p>
     */
    private static void exigerUnNomDUtilisateur(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Le nom d'utilisateur est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        if (username.trim().toLowerCase(java.util.Locale.ROOT).startsWith(PREFIXE_COMPTE_DE_SERVICE)) {
            throw new BusinessException("Le préfixe « " + PREFIXE_COMPTE_DE_SERVICE + " » est réservé aux "
                    + "comptes de service : choisissez un autre nom d'utilisateur.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Mot de passe fixé par un gestionnaire des utilisateurs.
     *
     * <p>Réservé à qui détient au moins tout ce que détient le compte : sinon, fixer le mot de passe
     * d'un administrateur suffisait à ouvrir sa session.</p>
     */
    public void fixerMotDePasseParGestionnaire(String userId, String password) {
        garde.exigerAscendantSur(userId, "réinitialiser son mot de passe");
        updatePassword(userId, password);
    }

    /**
     * Fixe le mot de passe, puis ferme toutes les sessions ouvertes du compte.
     *
     * <p>Un mot de passe se change parce qu'on le croit connu d'un autre : laisser vivantes les
     * sessions ouvertes avec l'ancien, c'était laisser l'intrus dedans jusqu'à l'expiration de son
     * jeton de renouvellement. Une déconnexion qui échoue ne défait pas le changement — le nouveau
     * mot de passe vaut déjà — elle est journalisée.</p>
     *
     * <p>Un mot de passe que la politique de Keycloak refuse est une erreur de l'appelant (400),
     * pas une panne.</p>
     */
    public void updatePassword(String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        UserResource compte = keycloak.realm(kcAuthProperties.getRealm()).users().get(userId);
        try {
            compte.resetPassword(credential);
        } catch (jakarta.ws.rs.BadRequestException e) {
            throw new BusinessException("Ce mot de passe ne respecte pas la politique de sécurité "
                    + "(longueur, caractères exigés, ou mot de passe déjà employé).", HttpStatus.BAD_REQUEST);
        }
        fermerLesSessions(compte, userId);
    }

    private void fermerLesSessions(UserResource compte, String userId) {
        try {
            compte.logout();
        } catch (Exception e) {
            log.warn("Mot de passe changé pour le compte {}, mais ses sessions n'ont pas pu être fermées : {}",
                    userId, e.getMessage());
        }
    }

    public void changeUserStatus(String userId, boolean enabled) {
        garde.exigerAscendantSur(userId, enabled ? "réactiver ce compte" : "désactiver ce compte");
        if (enabled) {
            // Réactiver reprend une place. Sans ce contrôle, le plafond se contournait en trois
            // gestes : désactiver, créer, réactiver.
            plafondDUtilisateurs.verifierAvantReactivation();
        }
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).toRepresentation();
        user.setEnabled(enabled);
        keycloak.realm(kcAuthProperties.getRealm()).users().get(userId).update(user);
    }

    public void emailVerification(String userId, String token) {
        // Le jeton du lien d'abord : sans lui, connaître l'identifiant d'un compte suffisait à
        // en marquer l'adresse vérifiée et à rendre permanent son mot de passe temporaire.
        jetons.consommer(userId, JetonDeCompte.Usage.VERIFICATION_COURRIEL, token);
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

    /**
     * Envoie un lien de réinitialisation à l'adresse, si un compte actif la porte.
     *
     * <p>La réponse est la même dans tous les cas : dire « aucun compte » permettrait de sonder
     * l'annuaire adresse par adresse. Une panne d'envoi se journalise sans se dire, pour la même
     * raison.</p>
     */
    public void initiatePasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        List<UserRepresentation> trouves = keycloak.realm(kcAuthProperties.getRealm()).users()
                .searchByEmail(email.trim(), true);
        if (trouves.size() != 1 || !Boolean.TRUE.equals(trouves.get(0).isEnabled())) {
            log.info("Réinitialisation demandée pour une adresse sans compte actif unique");
            return;
        }
        String userId = trouves.get(0).getId();
        String token = jetons.emettre(userId, JetonDeCompte.Usage.REINITIALISATION,
                JetonsDeCompte.DUREE_REINITIALISATION);
        String lien = frontendUrl + "/reset-password?token=" + token + "&userId=" + userId;
        try {
            sendMailService.sendReinitializePasswordEmail(trouves.get(0).getEmail(), lien);
        } catch (Exception e) {
            log.error("Lien de réinitialisation non envoyé au compte {} : {}", userId, e.getMessage());
        }
    }

    /** Fixe le nouveau mot de passe, sur présentation du jeton reçu par courriel — et de lui seul. */
    public void reinitializePwd(String userId, String password, String token) {
        jetons.consommer(userId, JetonDeCompte.Usage.REINITIALISATION, token);
        updatePassword(userId, password);
    }

    /**
     * Changement de mot de passe sur présentation de l'ancien.
     *
     * <p>Le contrôle que {@code username} est bien celui du demandeur se fait au contrôleur, qui
     * connaît la règle d'exception des gestionnaires. Ici : la règle d'ascendance d'abord, puis
     * l'ancien mot de passe, vérifié par une ouverture de session auprès de Keycloak ; son refus
     * (ou un compte inconnu) répond « ancien mot de passe incorrect ». Un nouveau mot de passe refusé par la politique de Keycloak le dit à son tour,
     * au lieu de se faire passer pour un ancien mot de passe faux.</p>
     *
     * <p>Quand le demandeur n'est pas le titulaire du compte, il doit détenir au moins tout ce que
     * le compte détient ({@link GardeDesHabilitations#exigerAscendantSur}).</p>
     */
    public ResponseEntity<Object> updatePassword(String username, String oldPassword, String password) {
        List<UserRepresentation> users = keycloak.realm(kcAuthProperties.getRealm())
                .users()
                .search(username, true);
        if (users.isEmpty()) {
            // Même réponse qu'un ancien mot de passe faux : la réponse ne dit pas si le compte existe.
            return ancienMotDePasseIncorrect();
        }
        String userId = users.get(0).getId();
        // Le contrôleur laisse un gestionnaire des utilisateurs viser un autre compte que le sien :
        // sans cette règle, il fixait le mot de passe d'un SUPER_ADMIN dont il aurait appris
        // l'ancien, sans jamais en détenir les habilitations. Même règle d'ascendance que pour la
        // réinitialisation ou le statut ; le titulaire du compte, lui, passe toujours.
        // Elle passe AVANT la vérification de l'ancien mot de passe : après, la réponse (401 si faux,
        // 403 si juste) faisait de ce point un oracle sur le mot de passe d'un compte plus habilité.
        garde.exigerAscendantSur(userId, "changer son mot de passe");

        try {
            kcTokenService.getAccessToken(KcLoginRequestDto.builder()
                    .username(username)
                    .password(oldPassword)
                    .build());
        } catch (Exception e) {
            log.info("Changement de mot de passe refusé : ancien mot de passe non reconnu ({})",
                    e.getClass().getSimpleName());
            return ancienMotDePasseIncorrect();
        }

        updatePassword(userId, password);

        return ResponseEntity.ok(KcResponseDto.builder()
                .status("SUCCESS")
                .message("Votre mot de passe a été mis à jour avec succès.")
                .build());
    }

    private static ResponseEntity<Object> ancienMotDePasseIncorrect() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(KcResponseDto.builder()
                        .status("ERROR")
                        .message("Ancien mot de passe incorrect.")
                        .build());
    }

    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        if (attributes != null && attributes.containsKey(key)) {
            List<String> values = attributes.get(key);
            if (!values.isEmpty()) {
                return values.get(0);
            }
        }
        return null;
    }

    /**
     * Renouvelle les jetons à partir du cookie {@code refresh_token}.
     *
     * <p>Un cookie absent ou un jeton que Keycloak refuse (session expirée, fermée après un
     * changement de mot de passe) répondait 500. C'est une session terminée : 401, que l'écran
     * traduit en retour à la connexion.</p>
     */
    public ResponseEntity<Object> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtils.getRefreshToken(request)
                .orElseThrow(() -> new BusinessException("Session expirée : reconnectez-vous.",
                        HttpStatus.UNAUTHORIZED));
        KcTokenDto kcTokenDto;
        try {
            kcTokenDto = kcTokenService.getRefreshToken(refreshToken);
        } catch (ReponseKeycloakEnErreur e) {
            if (e.refusDeLAppelant()) {
                throw new BusinessException("Session expirée : reconnectez-vous.", HttpStatus.UNAUTHORIZED);
            }
            throw e;
        }

        // Renouvellement des cookies avec les nouveaux tokens
        CookieUtils.addAccessTokenCookie(response, kcTokenDto.getAccessToken(), false);
        CookieUtils.addRefreshTokenCookie(response, kcTokenDto.getRefreshToken(), false);

        return ResponseEntity.ok().build();
    }

    /**
     * Déconnexion : efface les cookies HTTP-Only côté serveur.
     * Optionnellement révoque le token Keycloak (end_session).
     */
    public ResponseEntity<Object> logout(HttpServletRequest request, HttpServletResponse response) {
        // Révocation du refresh token côté Keycloak (best effort)
        CookieUtils.getRefreshToken(request).ifPresent(rt -> {
            try {
                kcTokenService.revokeToken(rt);
            } catch (Exception e) {
                log.warn("Impossible de révoquer le token Keycloak : {}", e.getMessage());
            }
        });

        // Effacement des cookies
        CookieUtils.clearAuthCookies(response, false);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Déconnexion réussie");
        return ResponseEntity.ok(result);
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

    /**
     * Retourne le profil complet d'un utilisateur par son ID Keycloak.
     * Inclut les rôles applicatifs, les permissions et les informations de licence.
     */
    public Map<String, Object> getUserById(String userId) {
        UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm())
                .users().get(userId).toRepresentation();

        Map<String, List<String>> attributes = user.getAttributes();
        String structureId = getAttributeValue(attributes, "structure");

        // Infos de licence via referentiel-service
        boolean licenseActive = false;
        int licenseDaysRemaining = 0;
        List<String> modulesSubscribed = new java.util.ArrayList<>();

        try {
            StructureDto direction = structureClient.getDirection();
            if (direction != null) {
                licenseActive = direction.getLicenceActive() != null && direction.getLicenceActive();
                licenseDaysRemaining = direction.getLicenseDaysRemaining() != null
                        ? direction.getLicenseDaysRemaining().intValue() : 0;
                modulesSubscribed = direction.getModulesSubscribed() != null
                        ? direction.getModulesSubscribed() : new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Erreur récupération licence pour getUserById: {}", e.getMessage());
        }

        // Rôles applicatifs
        List<String> appRoles = userRoleAssignmentRepository.findByUserId(user.getId()).stream()
                .map(assignment -> assignment.getRole().getName())
                .collect(Collectors.toList());

        boolean isSuperAdmin = appRoles.stream()
                .anyMatch(r -> r.equalsIgnoreCase("SUPER_ADMIN") || r.equalsIgnoreCase("SUPERADMIN"));
        if (isSuperAdmin && !appRoles.contains("SUPER_ADMIN")) {
            appRoles.add("SUPER_ADMIN");
        }

        List<String> permissions = computePermissions(appRoles, isSuperAdmin);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("firstName", user.getFirstName());
        result.put("lastName", user.getLastName());
        result.put("enabled", user.isEnabled());
        result.put("emailVerified", user.isEmailVerified());
        result.put("structure", structureId);
        result.put("fonction", getAttributeValue(attributes, "fonction"));
        // Le téléphone est écrit à la création du compte mais n'était rendu par aucune lecture :
        // l'écran « mon profil » ne pouvait donc pas afficher le numéro qu'il propose de changer.
        result.put("phoneNumber", getAttributeValue(attributes, "phoneNumber"));
        result.put("roles", appRoles);
        result.put("appRoles", appRoles);
        result.put("permissions", permissions);
        result.put("licenseActive", licenseActive);
        result.put("licenseDaysRemaining", licenseDaysRemaining);
        result.put("modulesSubscribed", modulesSubscribed);

        return result;
    }

    /**
     * Retourne le profil complet de l'utilisateur actuellement connecté, identifié par son jeton
     * vérifié.
     */
    public Map<String, Object> getMe() {
        return getUserById(monIdentifiant());
    }

    /**
     * Met à jour le profil de l'utilisateur connecté, et rend sa fiche relue.
     *
     * <p>L'identifiant vient du jeton, jamais du corps de la requête : l'y lire laisserait chacun
     * modifier le compte d'autrui en changeant un champ du formulaire. L'écriture elle-même est
     * confiée à {@link ProfilPersonnel}, qui la borne à trois champs — {@code updateUser}, prévu
     * pour l'administration, désactiverait ici le compte et effacerait les rôles.</p>
     */
    public Map<String, Object> mettreAJourMonProfil(com.qualiapproche.common.dto.auth.ProfilPersonnelDto profil) {
        String userId = monIdentifiant();
        profilPersonnel.mettreAJour(userId, profil);
        return getUserById(userId);
    }

    /**
     * L'identité du demandeur, lue dans le jeton que la chaîne de sécurité a <b>vérifié</b>.
     *
     * <p>Elle était tirée du cookie en décodant le jeton à la main, sans en vérifier la signature :
     * le point d'entrée était certes authentifié, mais rien ne garantissait que le jeton décodé ici
     * fût celui que la chaîne avait validé — l'en-tête {@code Authorization} et le cookie pouvaient
     * porter deux jetons différents.</p>
     */
    private String monIdentifiant() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("Vous devez vous connecter pour accéder à cette ressource.",
                    HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * Union (ou intersection filtrée) des permissions des rôles applicatifs de l'utilisateur.
     * SUPER_ADMIN reçoit l'union de toutes les permissions existantes, tous rôles confondus.
     */
    private List<String> computePermissions(List<String> appRoles, boolean isSuperAdmin) {
        if (isSuperAdmin) {
            return appRoleRepository.findAll().stream()
                    .flatMap(role -> role.getPermissions().stream()).distinct().collect(Collectors.toList());
        }
        return appRoleRepository.findAll().stream()
                .filter(role -> appRoles.contains(role.getName()))
                .flatMap(role -> role.getPermissions().stream()).distinct().collect(Collectors.toList());
    }

    /**
     * Rôles applicatifs de l'utilisateur connecté, identifiant et nom.
     *
     * <p>Les permissions seules ne suffisent pas à tous les appelants : le contrôle d'habilitation
     * d'une étape de workflow porte sur le rôle responsable, désigné par son identifiant. Or ni le
     * jeton ni {@code X-User-Permissions} ne transportent les rôles.</p>
     */
    public List<Map<String, String>> getMyRoles() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("Utilisateur non authentifié.");
        }
        return userRoleAssignmentRepository.findByUserId(userId).stream()
                .map(assignment -> assignment.getRole())
                .filter(java.util.Objects::nonNull)
                .map(role -> Map.of(
                        "id", role.getId() != null ? role.getId().toString() : "",
                        "name", role.getName() != null ? role.getName() : ""))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Utilisateurs joignables portant un rôle applicatif donné.
     *
     * <p>Résolution inverse de {@link #getMyRoles()}, dont dépendent les services qui notifient
     * les responsables d'une étape : jusqu'ici, aucun point d'entrée ne permettait de savoir
     * <b>qui</b> porte un rôle, et workflow-service se rabattait sur une adresse fabriquée à
     * partir du nom du rôle, qui ne correspondait à aucune boîte réelle.</p>
     *
     * <p>Le rôle est accepté indifféremment par identifiant ou par nom : les circuits de
     * validation le désignent tantôt de l'une, tantôt de l'autre façon.</p>
     *
     * <p>Sont écartés les utilisateurs désactivés et ceux sans adresse — les notifier est
     * impossible, et les compter comme destinataires masquerait le fait que personne n'est
     * prévenu.</p>
     *
     * <p>La structure, facultative, borne la réponse à ses membres. C'est elle qui empêche les
     * courriels d'étape d'arroser la plateforme : le supérieur à prévenir est celui de la
     * structure du dossier, pas tout porteur du rôle où qu'il soit. Les rôles à portée globale
     * — administration, responsabilité qualité — couvrent toutes les structures par définition :
     * la borne ne leur est pas opposée, sinon leurs étapes ne préviendraient plus personne dès
     * que le dossier vient d'une autre structure que la leur.</p>
     *
     * @param role        identifiant ou nom du rôle
     * @param structureId structure à laquelle restreindre, ou {@code null} pour ne pas restreindre
     * @return les destinataires, sans doublon ; liste vide si le rôle n'est porté par personne
     */
    public List<DestinataireDto> getUsersByRole(String role, String structureId) {
        if (role == null || role.isBlank()) {
            return List.of();
        }

        List<UserRoleAssignment> affectations =
                affectationsDuRole(role.trim());
        String structureExigee = structureExigee(affectations, structureId);

        return affectations.stream()
                .map(UserRoleAssignment::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(userId -> destinataire(userId, structureExigee))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Structure que les destinataires doivent partager, ou {@code null} si rien ne restreint.
     *
     * <p>Décidée ici et non chez l'appelant : le rôle lui est parfois désigné par identifiant, et
     * seul ce service sait le résoudre pour lire ce qu'il détient.</p>
     *
     * <p>La portée se lit sur la <b>permission</b> du rôle, non sur son nom. Une organisation nomme
     * ses rôles comme elle l'entend : un « Coordonnateur qualité » créé depuis l'écran était ignoré
     * par une comparaison de noms, et ses destinataires restaient bornés à une structure alors que
     * sa fonction ne l'est pas.</p>
     */
    private String structureExigee(List<UserRoleAssignment> affectations, String structureId) {
        if (structureId == null || structureId.isBlank()) {
            return null;
        }
        boolean porteeGlobale = affectations.stream()
                .map(UserRoleAssignment::getRole)
                .filter(java.util.Objects::nonNull)
                .map(com.qualiapproche.userservice.entities.AppRole::getPermissions)
                .filter(java.util.Objects::nonNull)
                .anyMatch(permissions -> permissions.contains(PermissionsPortee.TOUTES_STRUCTURES));
        return porteeGlobale ? null : structureId.trim();
    }

    /** Affectations du rôle, cherché par identifiant puis, à défaut, par nom. */
    private List<UserRoleAssignment> affectationsDuRole(String role) {
        try {
            List<UserRoleAssignment> parId =
                    userRoleAssignmentRepository.findByRole_Id(java.util.UUID.fromString(role));
            if (!parId.isEmpty()) {
                return parId;
            }
        } catch (IllegalArgumentException ignored) {
            // Le rôle n'est pas désigné par un identifiant : c'est un nom.
        }
        return userRoleAssignmentRepository.findByRole_NameIgnoreCase(role);
    }

    /**
     * Profil de contact d'un utilisateur, ou {@code null} s'il n'est pas joignable — ou s'il
     * n'appartient pas à la structure exigée.
     *
     * <p>Un compte peut avoir disparu de Keycloak sans que son affectation ait été retirée :
     * l'échec est alors propre à cet utilisateur et ne doit pas priver les autres de leur
     * notification.</p>
     */
    private DestinataireDto destinataire(String userId, String structureExigee) {
        try {
            UserRepresentation user = keycloak.realm(kcAuthProperties.getRealm())
                    .users().get(userId).toRepresentation();

            if (user == null || Boolean.FALSE.equals(user.isEnabled())) {
                return null;
            }
            if (structureExigee != null
                    && !structureExigee.equals(getAttributeValue(user.getAttributes(), "structure"))) {
                return null;
            }
            String email = user.getEmail();
            if (email == null || email.isBlank()) {
                log.warn("L'utilisateur {} porte le rôle demandé mais n'a pas d'adresse : "
                        + "il ne peut pas être notifié.", userId);
                return null;
            }

            String nomComplet = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                    + (user.getLastName() != null ? user.getLastName() : "")).trim();

            return DestinataireDto.builder()
                    .userId(userId)
                    .email(email)
                    .nomComplet(nomComplet.isBlank() ? email : nomComplet)
                    .build();
        } catch (Exception e) {
            log.warn("Profil de l'utilisateur {} illisible, il est écarté des destinataires : {}",
                    userId, e.getMessage());
            return null;
        }
    }

    /**
     * Permissions applicatives (AppRole) de l'utilisateur actuellement authentifié.
     * Appelé par la gateway (une fois par requête entrante vers un service protégé par
     * {@code @RequirePermissions}) pour les injecter dans l'en-tête interne
     * {@code X-User-Permissions} transmis aux services en aval.
     */
    public List<String> getMyPermissions() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("Utilisateur non authentifié.");
        }

        List<String> appRoles = userRoleAssignmentRepository.findByUserId(userId).stream()
                .map(assignment -> assignment.getRole().getName())
                .collect(Collectors.toList());

        boolean isSuperAdmin = appRoles.stream()
                .anyMatch(r -> r.equalsIgnoreCase("SUPER_ADMIN") || r.equalsIgnoreCase("SUPERADMIN"));

        return computePermissions(appRoles, isSuperAdmin);
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
