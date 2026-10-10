package com.qualiapproche.userservice.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.UserRoleAssignment;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import com.qualiapproche.userservice.service.AppRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rôles applicatifs : les permissions effectives de chacun.
 *
 * <p>La lecture reste ouverte (l'écran d'attribution et les circuits listent les rôles) ; créer,
 * modifier ou attribuer un rôle exige {@code ROLE_MANAGE}. C'était jusqu'ici ouvert à tout compte
 * connecté, qui pouvait donc se forger un rôle aux permissions de son choix et se l'attribuer.</p>
 */
@RestController
@RequestMapping("/api/v1/app-roles")
@RequirePermissions(
        create = {"ROLE_MANAGE"},
        update = {"ROLE_MANAGE"},
        read = {},
        delete = {"ROLE_MANAGE"}
)
@RequiredArgsConstructor
public class AppRoleController {

    private final AppRoleRepository appRoleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final AppRoleService appRoleService;
    private final ObjectMapper objectMapper;

    // Lire le dictionnaire des permissions depuis le JSON
    @GetMapping("/permissions-dictionary")
    public ResponseEntity<List<Map<String, String>>> getPermissionsDictionary() {
        try {
            InputStream is = new ClassPathResource("permissions.json").getInputStream();
            List<Map<String, String>> permissions = objectMapper.readValue(is, new TypeReference<>() { });
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public List<AppRole> getAllRoles() {
        return appRoleRepository.findAll();
    }

    /**
     * Création et modification partagent ce point d'entrée (le front y poste le rôle entier, avec
     * son identifiant lorsqu'il en a un). Le {@code save()} direct qui s'y trouvait acceptait
     * n'importe quelle réécriture, renommage compris ; les règles sont désormais portées par
     * {@link AppRoleService#enregistrer(AppRole)}.
     */
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping
    public AppRole createRole(@RequestBody AppRole role) {
        return appRoleService.enregistrer(role);
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PostMapping("/assign")
    public UserRoleAssignment assignRoleToUser(@RequestParam String userId, @RequestParam UUID roleId) {
        AppRole role = appRoleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        UserRoleAssignment assignment = UserRoleAssignment.builder()
                .userId(userId)
                .role(role)
                .build();

        return userRoleAssignmentRepository.save(assignment);
    }
}
