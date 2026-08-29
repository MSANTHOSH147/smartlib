package com.smartlib.controller;

import com.smartlib.dto.auth.AuthResponse;
import com.smartlib.dto.auth.LoginRequest;
import com.smartlib.dto.auth.RegisterRequest;
import com.smartlib.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartlib.entity.User;
import org.springframework.security.core.Authentication;
import com.smartlib.dto.auth.ChangePasswordRequest;
import com.smartlib.dto.auth.ForgotPasswordRequest;
import com.smartlib.dto.auth.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
    @GetMapping("/me")
public ResponseEntity<AuthResponse> getCurrentUser(
        Authentication authentication) {

    User user = (User) authentication.getPrincipal();

    return ResponseEntity.ok(
            AuthResponse.builder()
                    .token(null)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .build()
    );
}
// ============================================================
// CHANGE PASSWORD
// ============================================================

@PostMapping("/change-password")
public ResponseEntity<?> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        Authentication authentication) {

    User user =
            (User) authentication.getPrincipal();

    authService.changePassword(
            request,
            user
    );

    return ResponseEntity.ok(
            java.util.Map.of(
                    "success", true,
                    "message",
                    "Password changed successfully"
            )
    );
}


// ============================================================
// FORGOT PASSWORD
// ============================================================

@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request) {

    authService.forgotPassword(request);

    return ResponseEntity.ok(
            java.util.Map.of(
                    "success", true,
                    "message",
                    "If the email exists, a password reset email has been sent."
            )
    );
}


// ============================================================
// RESET PASSWORD
// ============================================================

@PostMapping("/reset-password")
public ResponseEntity<?> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request) {

    authService.resetPassword(request);

    return ResponseEntity.ok(
            java.util.Map.of(
                    "success", true,
                    "message",
                    "Password reset successfully"
            )
    );
}
}