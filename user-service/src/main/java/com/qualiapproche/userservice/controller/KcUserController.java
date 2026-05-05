package com.qualiapproche.userservice.controller;

import com.qualiapproche.common.dto.auth.KcLoginRequestDto;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.userservice.service.KcUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin("*")
@RequiredArgsConstructor
@Tag(name = "Gestion des Utilisateurs", description = "Endpoints pour l'authentification et la gestion des utilisateurs Keycloak")
public class KcUserController {
    private final KcUserService kcUserService;

    @Operation(summary = "Authentification utilisateur", description = "Permet de se connecter et de recevoir un jeton JWT")
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody KcLoginRequestDto loginRequest) {
        return ResponseEntity.ok(com.qualiapproche.common.dto.auth.KcResponseDto.builder()
                .status("SUCCESS")
                .data(kcUserService.login(loginRequest))
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Object> refreshToken(@RequestParam String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        return kcUserService.refreshToken(refreshToken, request, response);
    }

    @GetMapping("/users")
    public ResponseEntity<List<KcUserDto>> getAllUsers() {
        List<KcUserDto> users = kcUserService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/{structureId}")
    public ResponseEntity<List<KcUserDto>> getAllUsersByStructureId(@PathVariable String structureId) {
        List<KcUserDto> users = kcUserService.getUsersByStructure(structureId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/user-by-id")
    public ResponseEntity<KcUserDto> getUserById(@RequestParam String userId) {
        KcUserDto user = kcUserService.getUserById(userId);
        return ResponseEntity.ok(user);
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
