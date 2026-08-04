package com.seritracker.application.service;

import com.seritracker.domain.exception.InvalidRefreshTokenException;
import com.seritracker.domain.exception.InvalidResetTokenException;
import com.seritracker.domain.model.AuthResult;
import com.seritracker.domain.model.PasswordResetToken;
import com.seritracker.domain.model.RefreshToken;
import com.seritracker.domain.model.User;
import com.seritracker.domain.port.out.EmailSender;
import com.seritracker.domain.port.out.PasswordResetTokenRepository;
import com.seritracker.domain.port.out.RefreshTokenRepository;
import com.seritracker.domain.port.out.TokenService;
import com.seritracker.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenService tokenService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailSender emailSender;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUpConfig() {
        ReflectionTestUtils.setField(authService, "appBaseUrl", "http://localhost:4200");
        ReflectionTestUtils.setField(authService, "resetExpirationMinutes", 60L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 30L);
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .name("Test User")
                .email("test@test.com")
                .passwordHash("hashed_password")
                .role("USER")
                .build();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register user when email is not taken")
        void shouldRegisterUser_whenEmailIsNotTaken() {
            User saved = buildUser();

            when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(userRepository.save(any())).thenReturn(saved);
            when(tokenService.generateAccessToken(saved.getId(), saved.getEmail(), saved.getRole())).thenReturn("jwt_token");

            AuthResult result = authService.register("Test User", "test@test.com", "password123");

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("jwt_token");
            assertThat(result.getRefreshToken()).isNotBlank();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test User");
            verify(userRepository).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when email is already taken")
        void shouldThrowException_whenEmailIsAlreadyTaken() {
            when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register("Test User", "test@test.com", "password123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should encode password before saving")
        void shouldEncodePassword_beforeSaving() {
            User saved = buildUser();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(userRepository.save(any())).thenReturn(saved);
            when(tokenService.generateAccessToken(any(), anyString(), anyString())).thenReturn("jwt_token");

            authService.register("Test User", "test@test.com", "password123");

            verify(passwordEncoder).encode("password123");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should return token when credentials are valid")
        void shouldReturnToken_whenCredentialsAreValid() {
            User user = buildUser();

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
            when(tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole())).thenReturn("jwt_token");

            AuthResult result = authService.login("test@test.com", "password123");

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("jwt_token");
            assertThat(result.getRefreshToken()).isNotBlank();
            assertThat(result.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw BadCredentialsException when user does not exist")
        void shouldThrowBadCredentialsException_whenUserDoesNotExist() {
            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login("test@test.com", "password123"))
                    .isInstanceOf(BadCredentialsException.class);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("should throw BadCredentialsException when password is wrong")
        void shouldThrowBadCredentialsException_whenPasswordIsWrong() {
            User user = buildUser();

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.login("test@test.com", "password123"))
                    .isInstanceOf(BadCredentialsException.class);

            verify(tokenService, never()).generateAccessToken(any(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should update the password hash when the current password matches")
        void shouldUpdatePasswordHash_whenCurrentPasswordMatches() {
            User user = buildUser();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("newPassword456")).thenReturn("new_hashed_password");

            authService.changePassword(1L, "password123", "newPassword456");

            verify(userRepository).save(argThat(saved -> "new_hashed_password".equals(saved.getPasswordHash())));
            verify(refreshTokenRepository).deleteAllByUserId(1L);
        }

        @Test
        @DisplayName("should throw BadCredentialsException when the current password is wrong")
        void shouldThrowBadCredentialsException_whenCurrentPasswordIsWrong() {
            User user = buildUser();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(1L, "wrong", "newPassword456"))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BadCredentialsException when the user does not exist")
        void shouldThrowBadCredentialsException_whenUserDoesNotExist() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword(99L, "password123", "newPassword456"))
                    .isInstanceOf(BadCredentialsException.class);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("should create a reset token and send an email when the user exists")
        void shouldCreateTokenAndSendEmail_whenUserExists() {
            User user = buildUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            authService.forgotPassword(user.getEmail());

            verify(passwordResetTokenRepository).deleteByUserId(user.getId());
            verify(passwordResetTokenRepository).save(argThat(token ->
                    token.getUserId().equals(user.getId()) && token.getTokenHash() != null));
            verify(emailSender).send(eq(user.getEmail()), anyString(), anyString());
        }

        @Test
        @DisplayName("should do nothing when the email does not belong to any user")
        void shouldDoNothing_whenEmailIsUnknown() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            authService.forgotPassword("unknown@test.com");

            // Silencioso: no revela si el email existe o no
            verify(passwordResetTokenRepository, never()).save(any());
            verify(emailSender, never()).send(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        private PasswordResetToken buildToken(LocalDateTime expiresAt) {
            return PasswordResetToken.builder()
                    .id(10L)
                    .userId(1L)
                    .tokenHash("irrelevant_in_test_since_hash_is_computed_from_raw_token")
                    .expiresAt(expiresAt)
                    .createdAt(LocalDateTime.now().minusMinutes(5))
                    .build();
        }

        @Test
        @DisplayName("should update the password and delete the token when it is valid")
        void shouldUpdatePasswordAndDeleteToken_whenTokenIsValid() {
            PasswordResetToken token = buildToken(LocalDateTime.now().plusMinutes(30));
            User user = buildUser();

            when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPassword456")).thenReturn("new_hashed_password");

            authService.resetPassword("raw-token", "newPassword456");

            verify(userRepository).save(argThat(saved -> "new_hashed_password".equals(saved.getPasswordHash())));
            verify(passwordResetTokenRepository).deleteById(10L);
            verify(refreshTokenRepository).deleteAllByUserId(1L);
        }

        @Test
        @DisplayName("should throw InvalidResetTokenException when the token does not exist")
        void shouldThrowInvalidResetTokenException_whenTokenDoesNotExist() {
            when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword("bad-token", "newPassword456"))
                    .isInstanceOf(InvalidResetTokenException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidResetTokenException and delete the token when it is expired")
        void shouldThrowInvalidResetTokenException_whenTokenIsExpired() {
            PasswordResetToken token = buildToken(LocalDateTime.now().minusMinutes(1));
            when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.resetPassword("expired-token", "newPassword456"))
                    .isInstanceOf(InvalidResetTokenException.class);

            verify(passwordResetTokenRepository).deleteById(10L);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        private RefreshToken buildToken(LocalDateTime expiresAt) {
            return RefreshToken.builder()
                    .id(20L)
                    .userId(1L)
                    .tokenHash("irrelevant_in_test_since_hash_is_computed_from_raw_token")
                    .expiresAt(expiresAt)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();
        }

        @Test
        @DisplayName("should rotate the token and return a new access/refresh pair when valid")
        void shouldRotateToken_whenValid() {
            RefreshToken token = buildToken(LocalDateTime.now().plusDays(10));
            User user = buildUser();

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole())).thenReturn("new_jwt_token");

            AuthResult result = authService.refresh("raw-refresh-token");

            assertThat(result.getAccessToken()).isEqualTo("new_jwt_token");
            assertThat(result.getRefreshToken()).isNotBlank();
            verify(refreshTokenRepository).deleteByTokenHash(token.getTokenHash());
            verify(refreshTokenRepository).save(any());
        }

        @Test
        @DisplayName("should throw InvalidRefreshTokenException when the token does not exist")
        void shouldThrowInvalidRefreshTokenException_whenTokenDoesNotExist() {
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("bad-token"))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        @DisplayName("should throw InvalidRefreshTokenException and delete the token when it is expired")
        void shouldThrowInvalidRefreshTokenException_whenTokenIsExpired() {
            RefreshToken token = buildToken(LocalDateTime.now().minusDays(1));
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.refresh("expired-token"))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            verify(refreshTokenRepository).deleteByTokenHash(token.getTokenHash());
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("should revoke the given refresh token")
        void shouldRevokeRefreshToken() {
            authService.logout("raw-refresh-token");

            verify(refreshTokenRepository).deleteByTokenHash(anyString());
        }
    }
}
