package com.smartlib.service;

import com.smartlib.dto.auth.AuthResponse;
import com.smartlib.dto.auth.LoginRequest;
import com.smartlib.dto.auth.RegisterRequest;
import com.smartlib.entity.User;
import com.smartlib.enums.Role;
import com.smartlib.exception.BadRequestException;
import com.smartlib.exception.ResourceNotFoundException;
import com.smartlib.repository.PasswordResetTokenRepository;
import com.smartlib.repository.UserRepository;
import com.smartlib.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.smartlib.dto.auth.ChangePasswordRequest;
import com.smartlib.dto.auth.ForgotPasswordRequest;
import com.smartlib.dto.auth.ResetPasswordRequest;
import com.smartlib.entity.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {

            throw new BadRequestException(
                    "An account with this email already exists"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(Role.MEMBER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getId()
        );

        return buildResponse(savedUser, token);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(
                        request.getEmail().toLowerCase().trim()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invalid email or password"
                        ));

        if (!user.getActive()) {

            throw new BadRequestException(
                    "Your account is inactive"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new BadRequestException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return buildResponse(user, token);
    }

    private AuthResponse buildResponse(
            User user,
            String token) {

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
    // ============================================================
// CHANGE PASSWORD
// ============================================================

public void changePassword(
        ChangePasswordRequest request,
        User user) {

    if (!passwordEncoder.matches(
            request.getCurrentPassword(),
            user.getPassword())) {

        throw new BadRequestException(
                "Current password is incorrect"
        );
    }

    if (request.getCurrentPassword()
            .equals(request.getNewPassword())) {

        throw new BadRequestException(
                "New password must be different from current password"
        );
    }

    user.setPassword(
            passwordEncoder.encode(
                    request.getNewPassword()
            )
    );

    user.setUpdatedAt(LocalDateTime.now());

    userRepository.save(user);
}


// ============================================================
// FORGOT PASSWORD
// ============================================================

public void forgotPassword(
        ForgotPasswordRequest request) {

    String email =
            request.getEmail()
                    .toLowerCase()
                    .trim();

    User user =
            userRepository
                    .findByEmail(email)
                    .orElse(null);

    /*
     * Always return successfully even when
     * the email does not exist.
     *
     * This prevents account enumeration.
     */
    if (user == null) {
        return;
    }

    passwordResetTokenRepository
            .deleteByUserId(user.getId());

    String token =
            UUID.randomUUID().toString();

    PasswordResetToken resetToken =
            PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(
                            LocalDateTime.now()
                                    .plusMinutes(15)
                    )
                    .used(false)
                    .build();

    passwordResetTokenRepository.save(
            resetToken
    );

    emailService.sendPasswordResetEmail(
            user.getEmail(),
            token
    );
}


// ============================================================
// RESET PASSWORD
// ============================================================

public void resetPassword(
        ResetPasswordRequest request) {

    PasswordResetToken resetToken =
            passwordResetTokenRepository
                    .findByToken(request.getToken())
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Invalid password reset token"
                            ));

    if (resetToken.getUsed()) {

        throw new BadRequestException(
                "This password reset token has already been used"
        );
    }

    if (resetToken.getExpiresAt()
            .isBefore(LocalDateTime.now())) {

        throw new BadRequestException(
                "Password reset token has expired"
        );
    }

    User user =
            resetToken.getUser();

    user.setPassword(
            passwordEncoder.encode(
                    request.getNewPassword()
            )
    );

    user.setUpdatedAt(
            LocalDateTime.now()
    );

    userRepository.save(user);

    resetToken.setUsed(true);

    passwordResetTokenRepository.save(
            resetToken
    );
}
}