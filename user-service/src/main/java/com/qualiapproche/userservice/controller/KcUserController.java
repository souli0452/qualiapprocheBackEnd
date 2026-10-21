package com.qualiapproche.userservice.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.dto.auth.ChangementDeMotDePasseDto;
import com.qualiapproche.common.dto.auth.DemandeDeReinitialisationDto;
import com.qualiapproche.common.dto.auth.MotDePasseFixeParAdministrateurDto;
import com.qualiapproche.common.dto.auth.ReinitialisationParLienDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import com.qualiapproche.common.dto.auth.KcLoginRequestDto;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.common.dto.auth.ProfilPersonnelDto;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.userservice.service.KcUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import com.qualiapproche.common.dto.DestinataireDto;

/**
 * Comptes et authentification.
 *
 * <p>La lecture de l'annuaire reste ouverte à tout compte connecté : les sélecteurs d'utilisateurs
 * de tous les modules s'en servent. Créer, modifier, désactiver un compte ou en fixer le mot de
 * passe exige {@code MANAGE_USER} — jusqu'ici, n'importe quel agent connecté pouvait le faire,
 * y compris sur le compte d'un administrateur.</p>
 *
 * <p>Mots de passe, jetons de lien et adresses voyagent dans le <b>corps</b> des requêtes, jamais
 * dans l'URL (CWE-598) : une URL s'inscrit dans les journaux d'accès de chaque intermédiaire, dans
 * l'historique du navigateur et dans l'en-tête {@code Referer}. Seul {@code /verify-email} garde ses
 * paramètres : il est appelé depuis le lien du courriel, et son jeton ne sert qu'une fois.</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequirePermissions(
        create = {"MANAGE_USER"},
        update = {"MANAGE_USER"},
        read = {},
        delete = {"MANAGE_USER"}
)
//@CrossOrigin("*")
@RequiredArgsConstructor
@Tag(name = "Gestion des Utilisateurs", description = "Endpoints pour l'authentification et la gestion des utilisateurs Keycloak")
public class KcUserController {
    private final KcUserService kcUserService;
    private final PermissionChecker perm;

    @Operation(summary = "Authentification utilisateur",
            description = "Permet de se connecter. Les tokens JWT sont déposés dans des cookies HTTP-Only sécurisés.")
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody KcLoginRequestDto loginRequest,
                                        HttpServletResponse response) {
        return ResponseEntity.ok(kcUserService.login(loginRequest, response));
    }

    @Operation(summary = "Rafraîchissement du token",
            description = "Lit le refresh_token depuis le cookie et émet un nouvel access_token via cookie HTTP-Only.")
    @PostMapping("/refresh")
    public ResponseEntity<Object> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        return kcUserService.refreshToken(request, response);
    }

    @Operation(summary = "Déconnexion", description = "Efface les cookies d'authentification et révoque le token Keycloak.")
    @PostMapping("/logout")
    public ResponseEntity<Object> logout(HttpServletRequest request, HttpServletResponse response) {
        return kcUserService.logout(request, response);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<KcUserDto>> getAllUsers(@ParameterObject Pageable pageable) {
        Page<KcUserDto> users = kcUserService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{structureId}")
    public ResponseEntity<Page<KcUserDto>> getAllUsersByStructureId(@PathVariable String structureId, @ParameterObject Pageable pageable) {
        Page<KcUserDto> users = kcUserService.getUsersByStructure(structureId, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/user-by-id")
    public ResponseEntity<java.util.Map<String, Object>> getUserById(@RequestParam String userId) {
        java.util.Map<String, Object> user = kcUserService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Profil de l'utilisateur connecté",
            description = "Retourne le profil complet de l'utilisateur connecté en lisant l'access_token depuis les cookies HTTP-Only.")
    @GetMapping("/me")
    public ResponseEntity<java.util.Map<String, Object>> getMe() {
        return ResponseEntity.ok(kcUserService.getMe());
    }

    /**
     * Libre-service : l'utilisateur change son nom, son prénom et son téléphone.
     *
     * <p>Distinct de {@code PUT /users/update}, qui sert l'administration des comptes et porte
     * aussi l'activation, la structure et les rôles. Employé depuis un écran de profil, dont le
     * formulaire ne porte aucun de ces champs, il désactiverait le compte de l'intéressé et
     * effacerait ses rôles applicatifs — un {@code boolean} primitif absent du corps vaut
     * {@code false}, et une liste de rôles absente vaut « aucun ».</p>
     *
     * <p>Aucune habilitation n'est exigée, et il n'en faut pas : l'identité écrite est celle du
     * jeton, si bien qu'on ne peut modifier que son propre compte.</p>
     */
    @Operation(summary = "Modifier son propre profil",
            description = "Met à jour le nom, le prénom et le téléphone de l'utilisateur connecté, "
                    + "et retourne sa fiche relue. L'identité vient de l'access_token : ni rôle, ni "
                    + "structure, ni activation ne peuvent être touchés par cette voie.")
    @PutMapping("/me")
    public ResponseEntity<java.util.Map<String, Object>> mettreAJourMonProfil(
            @RequestBody ProfilPersonnelDto profil) {
        return ResponseEntity.ok(kcUserService.mettreAJourMonProfil(profil));
    }

    /**
     * Retourne explicitement un {@link ApiResponse} (et non une {@code List} nue) pour échapper
     * à {@code GlobalResponseHandler}, qui applique sinon une pagination automatique (page=0,
     * size=10) à toute réponse de type List — inadapté ici : cette liste ne doit jamais être
     * tronquée, la gateway a besoin de l'ensemble complet des permissions.
     */
    @Operation(summary = "Permissions de l'utilisateur connecté",
            description = "Usage interne : appelé par la gateway pour propager les permissions "
                    + "applicatives (AppRole) aux services en aval via l'en-tête X-User-Permissions.")
    @GetMapping("/me/permissions")
    public ResponseEntity<ApiResponse<List<String>>> getMyPermissions() {
        return ResponseEntity.ok(ApiResponse.success(kcUserService.getMyPermissions()));
    }

    /**
     * Même précaution que {@code /me/permissions} : {@link ApiResponse} explicite pour échapper à
     * la pagination automatique appliquée par {@code GlobalResponseHandler} à toute List nue.
     */
    @Operation(summary = "Rôles de l'utilisateur connecté",
            description = "Usage interne : permet aux services aval de contrôler une habilitation "
                    + "exprimée en rôle, que ni le jeton ni X-User-Permissions ne transportent.")
    @GetMapping("/me/roles")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, String>>>> getMyRoles() {
        return ResponseEntity.ok(ApiResponse.success(kcUserService.getMyRoles()));
    }

    /**
     * Résolution inverse de {@code /me/roles} : qui porte ce rôle ?
     *
     * <p>Usage interne. Sans elle, un service qui doit prévenir les responsables d'une étape n'a
     * aucun moyen de connaître leurs adresses — workflow-service en fabriquait une à partir du
     * nom du rôle, qui ne correspondait à aucune boîte réelle.</p>
     *
     * <p>Enveloppé dans {@code ApiResponse} comme {@code /me/roles}, pour échapper à la
     * pagination automatique appliquée par {@code GlobalResponseHandler} à toute List nue.</p>
     */
    @Operation(summary = "Utilisateurs portant un rôle",
            description = "Usage interne : permet aux services aval de notifier les porteurs d'un rôle. "
                    + "Le rôle est accepté par identifiant ou par nom. La structure, facultative, "
                    + "borne la réponse à ses membres — sauf pour les rôles à portée globale.")
    @GetMapping("/roles/{role}/users")
    public ResponseEntity<ApiResponse<List<DestinataireDto>>> getUsersByRole(
            @PathVariable("role") String role,
            @RequestParam(value = "structureId", required = false) String structureId) {
        return ResponseEntity.ok(ApiResponse.success(kcUserService.getUsersByRole(role, structureId)));
    }

    /** Un compte de service crée aussi des comptes : l'administrateur d'une organisation, à son provisionnement. */
    @PreAuthorize("@perm.canCreate(this) or @perm.appelDeService()")
    @PostMapping("/users/create")
    public ResponseEntity<KcUserDto> createUser(@RequestBody KcUserDto kcUserDto) {
        return ResponseEntity.ok(kcUserService.createUser(kcUserDto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/users/update")
    public ResponseEntity<Void> updateUser(@RequestBody KcUserDto kcUserDto) {
        kcUserService.updateUser(kcUserDto);
        return ResponseEntity.ok().build();
    }

    /** Un gestionnaire fixe le mot de passe d'un compte qui n'est pas plus privilégié que lui. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PatchMapping("/users/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody MotDePasseFixeParAdministrateurDto corps) {
        kcUserService.fixerMotDePasseParGestionnaire(corps.userId(), corps.password());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/users/change-status")
    public ResponseEntity<Void> changeStatus(@RequestParam String userId, @RequestParam Boolean enabled) {
        kcUserService.changeUserStatus(userId, enabled);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String userId, @RequestParam String token) {
        kcUserService.emailVerification(userId, token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/is-email-verified")
    public boolean isEmailVerified(@RequestParam String userId) {
        return kcUserService.isEmailVerified(userId);
    }

    /**
     * Demande d'un lien de réinitialisation. Même réponse que l'adresse ait un compte ou non.
     * Le type {@code String} déclaré ici faisait échouer l'enveloppe de réponse : 500 à chaque appel.
     */
    @PostMapping("/initiate-reset-pwd")
    public ResponseEntity<Void> initiatePasswordReset(@Valid @RequestBody DemandeDeReinitialisationDto corps) {
        kcUserService.initiatePasswordReset(corps.email());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reinitialize-pwd")
    public ResponseEntity<Void> reinitializePwd(@Valid @RequestBody ReinitialisationParLienDto corps) {
        kcUserService.reinitializePwd(corps.userId(), corps.password(), corps.token());
        return ResponseEntity.ok().build();
    }

    /**
     * Changement de mot de passe sur présentation de l'ancien.
     *
     * <p>Le nom de compte venait de la requête sans être confronté au jeton : tout utilisateur
     * connecté pouvait éprouver des mots de passe sur le compte de son choix, la réponse disant
     * lesquels étaient justes. Le compte visé doit désormais être celui du jeton
     * ({@code preferred_username}) ; seul un gestionnaire des utilisateurs — la règle de
     * {@code @perm.canUpdate} — peut en viser un autre.</p>
     */
    @PutMapping("/update-pwd")
    public ResponseEntity<Object> updateTemporaryPassword(@Valid @RequestBody ChangementDeMotDePasseDto corps) {
        exigerSonPropreCompte(corps.username());
        return kcUserService.updatePassword(corps.username(), corps.oldPassword(), corps.password());
    }

    private void exigerSonPropreCompte(String username) {
        Jwt jwt = SecurityUtils.getJwt();
        String moi = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        // Keycloak range les noms de connexion en minuscules : la comparaison ne tient pas à la casse.
        if (moi != null && moi.equalsIgnoreCase(username.trim())) {
            return;
        }
        if (perm.canUpdate(this)) {
            return;
        }
        throw new BusinessException("Vous ne pouvez changer que le mot de passe de votre propre compte.",
                HttpStatus.FORBIDDEN);
    }
}
