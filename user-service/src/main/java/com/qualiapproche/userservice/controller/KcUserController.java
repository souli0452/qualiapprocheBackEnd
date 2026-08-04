package com.qualiapproche.userservice.controller;

import com.qualiapproche.common.dto.auth.KcLoginRequestDto;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.userservice.service.KcUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/v1")
//@CrossOrigin("*")
@RequiredArgsConstructor
@Tag(name = "Gestion des Utilisateurs", description = "Endpoints pour l'authentification et la gestion des utilisateurs Keycloak")
public class KcUserController {
    private final KcUserService kcUserService;

    @Operation(summary = "Authentification utilisateur", description = "Permet de se connecter. Les tokens JWT sont déposés dans des cookies HTTP-Only sécurisés.")
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody KcLoginRequestDto loginRequest,
                                        HttpServletResponse response) {
        return ResponseEntity.ok(kcUserService.login(loginRequest, response));
    }

    @Operation(summary = "Rafraîchissement du token", description = "Lit le refresh_token depuis le cookie et émet un nouvel access_token via cookie HTTP-Only.")
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

    @Operation(summary = "Profil de l'utilisateur connecté", description = "Retourne le profil complet de l'utilisateur connecté en lisant l'access_token depuis les cookies HTTP-Only.")
    @GetMapping("/me")
    public ResponseEntity<java.util.Map<String, Object>> getMe(HttpServletRequest request) {
        return ResponseEntity.ok(kcUserService.getMe(request));
    }

    /**
     * Retourne explicitement un {@link ApiResponse} (et non une {@code List} nue) pour échapper
     * à {@code GlobalResponseHandler}, qui applique sinon une pagination automatique (page=0,
     * size=10) à toute réponse de type List — inadapté ici : cette liste ne doit jamais être
     * tronquée, la gateway a besoin de l'ensemble complet des permissions.
     */
    @Operation(summary = "Permissions de l'utilisateur connecté", description = "Usage interne : appelé par la gateway pour propager les permissions applicatives (AppRole) aux services en aval via l'en-tête X-User-Permissions.")
    @GetMapping("/me/permissions")
    public ResponseEntity<ApiResponse<List<String>>> getMyPermissions() {
        return ResponseEntity.ok(ApiResponse.success(kcUserService.getMyPermissions()));
    }

    /**
     * Même précaution que {@code /me/permissions} : {@link ApiResponse} explicite pour échapper à
     * la pagination automatique appliquée par {@code GlobalResponseHandler} à toute List nue.
     */
    @Operation(summary = "Rôles de l'utilisateur connecté", description = "Usage interne : permet aux services aval de contrôler une habilitation exprimée en rôle, que ni le jeton ni X-User-Permissions ne transportent.")
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
                    + "Le rôle est accepté par identifiant ou par nom.")
    @GetMapping("/roles/{role}/users")
    public ResponseEntity<ApiResponse<List<com.qualiapproche.common.dto.DestinataireDto>>> getUsersByRole(
            @PathVariable("role") String role) {
        return ResponseEntity.ok(ApiResponse.success(kcUserService.getUsersByRole(role)));
    }

    @PostMapping("/users/create")
    public ResponseEntity<KcUserDto> createUser(@RequestBody KcUserDto kcUserDto) {
        return ResponseEntity.ok(kcUserService.createUser(kcUserDto));
    }

    @PutMapping("/users/update")
    public ResponseEntity<Void> updateUser(@RequestBody KcUserDto kcUserDto) {
        kcUserService.updateUser(kcUserDto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestParam String userId, @RequestParam String password) {
        kcUserService.updatePassword(userId, password);
        return ResponseEntity.ok().build();
    }

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

    @PostMapping("/initiate-reset-pwd")
    public ResponseEntity<String> initiatePasswordReset(@RequestParam String email) {
            kcUserService.initiatePasswordReset(email);
            return ResponseEntity.ok().build();
    }

    @PutMapping("/reinitialize-pwd")
    public ResponseEntity<Void> reinitializePwd(@RequestParam String userId,@RequestParam String password, @RequestParam String token) {
        kcUserService.reinitializePwd(userId,password, token);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update-pwd")
    public ResponseEntity<Object> updateTemporaryPassword(
            @RequestParam String username,
            @RequestParam(required = false) String oldPassword,
            @RequestParam String password) {
        return kcUserService.updatePassword(username, oldPassword, password);
    }
}
