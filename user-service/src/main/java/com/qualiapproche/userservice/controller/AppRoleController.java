package com.qualiapproche.userservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.UserRoleAssignment;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/app-roles")
@RequiredArgsConstructor
public class AppRoleController {

    private final AppRoleRepository appRoleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final ObjectMapper objectMapper;

    // Lire le dictionnaire des permissions depuis le JSON
    @GetMapping("/permissions-dictionary")
    public ResponseEntity<List<Map<String, String>>> getPermissionsDictionary() {
        try {
            InputStream is = new ClassPathResource("permissions.json").getInputStream();
            List<Map<String, String>> permissions = objectMapper.readValue(is, new TypeReference<>() {});
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public List<AppRole> getAllRoles() {
        return appRoleRepository.findAll();
    }

    @PostMapping
    public AppRole createRole(@RequestBody AppRole role) {
        return appRoleRepository.save(role);
    }

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
