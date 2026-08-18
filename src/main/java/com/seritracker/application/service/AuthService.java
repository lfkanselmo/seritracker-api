package com.seritracker.application.service;

import com.seritracker.domain.exception.InvalidRefreshTokenException;
import com.seritracker.domain.exception.InvalidResetTokenException;
import com.seritracker.domain.model.AuthResult;
import com.seritracker.domain.model.PasswordResetToken;
import com.seritracker.domain.model.RefreshToken;
import com.seritracker.domain.model.User;
import com.seritracker.domain.port.in.AuthUseCase;
import com.seritracker.domain.port.out.EmailSender;
import com.seritracker.domain.port.out.PasswordResetTokenRepository;
import com.seritracker.domain.port.out.RefreshTokenRepository;
import com.seritracker.domain.port.out.TokenService;
import com.seritracker.domain.port.out.UserRepository;
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
public class AuthService implements AuthUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailSender emailSender;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${password-reset.expiration-minutes}")
    private long resetExpirationMinutes;

    @Value("${refresh-token.expiration-days}")
    private long refreshTokenExpirationDays;

    @Override
    public AuthResult register(String name, String email, String password) {
        log.info("Registering new user");

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed — email already registered");
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role("USER")
                .build();

        User saved = userRepository.save(user);
        log.info("User id={} registered successfully", saved.getId());

        return buildAuthResult(saved);
    }

    @Override
    public AuthResult login(String email, String password) {
        log.info("Login attempt");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed — user not found");
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed — invalid password for userId={}", user.getId());
            throw new BadCredentialsException("Invalid credentials");
        }

        log.info("User id={} logged in successfully", user.getId());
        return buildAuthResult(user);
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        log.info("Attempting token refresh");

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByTokenHash(token.getTokenHash());
            log.warn("Token refresh failed — refresh token expired for userId={}", token.getUserId());
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);

        // Rotación: el refresh token usado queda inválido, se emite uno nuevo.
        refreshTokenRepository.deleteByTokenHash(token.getTokenHash());

        log.info("Token refreshed successfully for userId={}", user.getId());
        return buildAuthResult(user);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByTokenHash(hash(refreshToken));
        log.info("Logout — refresh token revoked");
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Changing password for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("Password change failed — current password mismatch for userId={}", userId);
            throw new BadCredentialsException("Current password is incorrect");
        }

        userRepository.save(user.withPasswordHash(passwordEncoder.encode(newPassword)));
        refreshTokenRepository.deleteAllByUserId(userId);
        log.info("Password changed successfully for userId={} — all sessions revoked", userId);
    }

    @Override
    public void forgotPassword(String email) {
        log.info("Password reset requested");

        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            // No revelamos si el email existe o no — misma respuesta para ambos casos.
            log.info("Password reset requested for an email that is not registered");
            return;
        }

        User user = maybeUser.get();
        String rawToken = issueResetToken(user);
        sendResetEmail(user, rawToken);

        log.info("Password reset email dispatched for userId={}", user.getId());
    }

    private String issueResetToken(User user) {
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String rawToken = generateRawToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(resetExpirationMinutes))
                .build();
        passwordResetTokenRepository.save(token);
        return rawToken;
    }

    private void sendResetEmail(User user, String rawToken) {
        String resetLink = appBaseUrl + "/auth/reset-password?token=" + rawToken;
        emailSender.send(
                user.getEmail(),
                "Recuperar contraseña — SeriesTracker",
                "Hacé clic en el siguiente link para restablecer tu contraseña. " +
                        "Expira en " + resetExpirationMinutes + " minutos:\n\n" + resetLink +
                        "\n\nSi no pediste este cambio, ignorá este mensaje."
        );
    }

    @Override
    public void resetPassword(String rawToken, String newPassword) {
        log.info("Attempting password reset with token");

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidResetTokenException::new);

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.deleteById(token.getId());
            log.warn("Password reset failed — token expired for userId={}", token.getUserId());
            throw new InvalidResetTokenException();
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidResetTokenException::new);

        userRepository.save(user.withPasswordHash(passwordEncoder.encode(newPassword)));
        passwordResetTokenRepository.deleteById(token.getId());
        refreshTokenRepository.deleteAllByUserId(user.getId());

        log.info("Password reset successfully for userId={} — all sessions revoked", user.getId());
    }

    private AuthResult buildAuthResult(User user) {
        String accessToken = tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = createRefreshToken(user.getId());

        return AuthResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .name(user.getName())
                .userId(user.getId())
                .build();
    }

    private String createRefreshToken(Long userId) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
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
