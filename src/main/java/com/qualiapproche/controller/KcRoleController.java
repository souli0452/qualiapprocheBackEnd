package com.qualiapproche.controller;

import com.qualiapproche.dto.auth.KcRoleDto;
import com.qualiapproche.service.kcService.KcRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.qualiapproche.utils.ApiUrls.URL_ROLES;

@Slf4j
@RestController
@RequestMapping(URL_ROLES)
@RequiredArgsConstructor

public class KcRoleController {

    @Autowired
    private KcRoleService kcRoleService;

    @GetMapping
    public ResponseEntity<List<KcRoleDto>> getAllRoles() {
        return ResponseEntity.ok(kcRoleService.getAllRoles());
    }

    @GetMapping("/{roleName}")
    public ResponseEntity<KcRoleDto> getRoleByName(@RequestParam String roleName) {
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

