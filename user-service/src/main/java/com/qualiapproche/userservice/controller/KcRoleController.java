package com.qualiapproche.userservice.controller;

import com.qualiapproche.common.dto.auth.KcRoleDto;
import com.qualiapproche.userservice.service.KcRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Rôles", description = "Gestion des rôles et des habilitations Keycloak")
public class KcRoleController {

    private final KcRoleService kcRoleService;

    @Operation(summary = "Lister les rôles", description = "Récupère tous les rôles définis dans Keycloak")
    @GetMapping
    public ResponseEntity<List<KcRoleDto>> getAllRoles() {
        return ResponseEntity.ok(kcRoleService.getAllRoles());
    }

    @GetMapping("/{roleName}")
    public ResponseEntity<KcRoleDto> getRoleByName(@PathVariable String roleName) {
        return ResponseEntity.ok(kcRoleService.getRoleByName(roleName));
    }

    @PostMapping
    public ResponseEntity<KcRoleDto> createRole(@RequestBody KcRoleDto kcRoleDto) {
        KcRoleDto createdRole = kcRoleService.createRole(kcRoleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updateRole(@RequestBody KcRoleDto kcRoleDto) {
        kcRoleService.updateRole(kcRoleDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roleName}")
    public ResponseEntity<Void> deleteRole(@PathVariable String roleName) {
        kcRoleService.deleteRole(roleName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/role/{id}")
    public ResponseEntity<Void> deleteRoleById(@PathVariable UUID id) {
        try {
            kcRoleService.deleteRoleById(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/assign-roles")
    public ResponseEntity<Void> assignRoles(@RequestParam String userId, @RequestBody List<String> roleNames) {
        kcRoleService.assignRoles(userId, roleNames);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user-roles/{userId}")
    public List<KcRoleDto> getRoles(@PathVariable String userId) {
        return kcRoleService.getRolesForUser(userId);
    }
}
