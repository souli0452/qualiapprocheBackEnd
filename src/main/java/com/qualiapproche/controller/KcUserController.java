package com.qualiapproche.controller;

import com.qualiapproche.dto.auth.KcLoginRequestDto;
import com.qualiapproche.dto.auth.KcUserDto;
import com.qualiapproche.service.kcService.KcUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.qualiapproche.utils.ApiUrls.QUALI_APPROCHE_ROOT_URL;


@Slf4j
@RestController
@RequestMapping(QUALI_APPROCHE_ROOT_URL)
@CrossOrigin("*")
@RequiredArgsConstructor
public class KcUserController {
    private final KcUserService kcUserService;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody KcLoginRequestDto loginRequest, HttpServletResponse response) {
        return kcUserService.login(loginRequest, response);
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
        log.debug("Request to create user : {}", kcUserDto);
        return ResponseEntity.ok(kcUserService.createUser(kcUserDto));
    }

    @PutMapping("/users/update")
    public ResponseEntity<Void> updateUser(@RequestBody KcUserDto kcUserDto) {
        log.debug("Request to update user : {}", kcUserDto);
        kcUserService.updateUser(kcUserDto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestParam String userId, @RequestParam String password) {
        log.debug("Request to reset password for user : {}", userId);
        kcUserService.updatePassword(userId, password);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/change-status")
    public ResponseEntity<Void> changeStatus(@RequestParam String userId, @RequestParam Boolean enabled) {
        log.debug("Request to reset password for user : {}", userId);
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
            @RequestParam(required = false) String  oldPassword,
            @RequestParam String password) {
        return kcUserService.updatePassword(username, oldPassword, password);
    }
}
