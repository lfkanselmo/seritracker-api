package com.seritracker.application.service;

import com.seritracker.domain.exception.InvalidResetTokenException;
import com.seritracker.domain.model.PasswordResetToken;
import com.seritracker.domain.model.User;
import com.seritracker.domain.port.out.EmailSender;
import com.seritracker.domain.port.out.PasswordResetTokenRepository;
import com.seritracker.domain.port.out.UserRepository;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ChangePasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ForgotPasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ResetPasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.AuthResponse;
import com.seritracker.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailSender emailSender;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${password-reset.expiration-minutes}")
    private long resetExpirationMinutes;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user");

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already registered");
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();

        User saved = userRepository.save(user);
        log.info("User id={} registered successfully", saved.getId());

        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), saved.getRole());
        return AuthResponse.builder()
                .token(token)
                .email(saved.getEmail())
                .name(saved.getName())
                .userId(saved.getId())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — user not found");
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed — invalid password for userId={}", user.getId());
            throw new BadCredentialsException("Invalid credentials");
        }

        log.info("User id={} logged in successfully", user.getId());
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .userId(user.getId())
                .build();
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Changing password for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Password change failed — current password mismatch for userId={}", userId);
            throw new BadCredentialsException("Current password is incorrect");
        }

        userRepository.save(user.withPasswordHash(passwordEncoder.encode(request.getNewPassword())));
        log.info("Password changed successfully for userId={}", userId);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Password reset requested");

        Optional<User> maybeUser = userRepository.findByEmail(request.getEmail());
        if (maybeUser.isEmpty()) {
            // No revelamos si el email existe o no — misma respuesta para ambos casos.
            log.info("Password reset requested for an email that is not registered");
            return;
        }

        User user = maybeUser.get();
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String rawToken = generateRawToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(resetExpirationMinutes))
                .build();
        passwordResetTokenRepository.save(token);

        String resetLink = appBaseUrl + "/auth/reset-password?token=" + rawToken;
        emailSender.send(
                user.getEmail(),
                "Recuperar contraseña — SeriesTracker",
                "Hacé clic en el siguiente link para restablecer tu contraseña. " +
                        "Expira en " + resetExpirationMinutes + " minutos:\n\n" + resetLink +
                        "\n\nSi no pediste este cambio, ignorá este mensaje."
        );

        log.info("Password reset email dispatched for userId={}", user.getId());
    }

    public void resetPassword(ResetPasswordRequest request) {
        log.info("Attempting password reset with token");

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(request.getToken()))
                .orElseThrow(InvalidResetTokenException::new);

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.deleteById(token.getId());
            log.warn("Password reset failed — token expired for userId={}", token.getUserId());
            throw new InvalidResetTokenException();
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidResetTokenException::new);

        userRepository.save(user.withPasswordHash(passwordEncoder.encode(request.getNewPassword())));
        passwordResetTokenRepository.deleteById(token.getId());

        log.info("Password reset successfully for userId={}", user.getId());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}