package com.seritracker.application.service;

import com.seritracker.domain.model.User;
import com.seritracker.domain.port.out.UserRepository;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ChangePasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.AuthResponse;
import com.seritracker.infrastructure.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    // ── Factories ──────────────────────────────────────────────────────

    private User buildUser() {
        return User.builder()
                .id(1L)
                .name("Test User")
                .email("test@test.com")
                .passwordHash("hashed_password")
                .role("USER")
                .build();
    }

    private RegisterRequest buildRegisterRequest() {
        return new RegisterRequest("Test User", "test@test.com", "password123");
    }

    private LoginRequest buildLoginRequest() {
        return new LoginRequest("test@test.com", "password123");
    }

    // ── register ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register user when email is not taken")
        void shouldRegisterUser_whenEmailIsNotTaken() {
            // Arrange
            RegisterRequest request = buildRegisterRequest();
            User saved = buildUser();

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed_password");
            when(userRepository.save(any())).thenReturn(saved);
            when(jwtService.generateToken(saved.getId(), saved.getEmail(), saved.getRole())).thenReturn("jwt_token");

            // Act
            AuthResponse result = authService.register(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("jwt_token");
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test User");
            verify(userRepository).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when email is already taken")
        void shouldThrowException_whenEmailIsAlreadyTaken() {
            // Arrange
            RegisterRequest request = buildRegisterRequest();
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should encode password before saving")
        void shouldEncodePassword_beforeSaving() {
            // Arrange
            RegisterRequest request = buildRegisterRequest();
            User saved = buildUser();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed_password");
            when(userRepository.save(any())).thenReturn(saved);
            when(jwtService.generateToken(any(), anyString(), anyString())).thenReturn("jwt_token");

            // Act
            authService.register(request);

            // Assert
            verify(passwordEncoder).encode(request.getPassword());
        }
    }

    // ── login ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should return token when credentials are valid")
        void shouldReturnToken_whenCredentialsAreValid() {
            // Arrange
            LoginRequest request = buildLoginRequest();
            User user = buildUser();

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
            when(jwtService.generateToken(user.getId(), user.getEmail(), user.getRole())).thenReturn("jwt_token");

            // Act
            AuthResponse result = authService.login(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("jwt_token");
            assertThat(result.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw BadCredentialsException when user does not exist")
        void shouldThrowBadCredentialsException_whenUserDoesNotExist() {
            // Arrange
            LoginRequest request = buildLoginRequest();
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("should throw BadCredentialsException when password is wrong")
        void shouldThrowBadCredentialsException_whenPasswordIsWrong() {
            // Arrange
            LoginRequest request = buildLoginRequest();
            User user = buildUser();

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateToken(any(), anyString(), anyString());
        }
    }

    // ── changePassword ────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should update the password hash when the current password matches")
        void shouldUpdatePasswordHash_whenCurrentPasswordMatches() {
            // Arrange
            User user = buildUser();
            ChangePasswordRequest request = new ChangePasswordRequest("password123", "newPassword456");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("newPassword456")).thenReturn("new_hashed_password");

            // Act
            authService.changePassword(1L, request);

            // Assert
            verify(userRepository).save(argThat(saved -> "new_hashed_password".equals(saved.getPasswordHash())));
        }

        @Test
        @DisplayName("should throw BadCredentialsException when the current password is wrong")
        void shouldThrowBadCredentialsException_whenCurrentPasswordIsWrong() {
            // Arrange
            User user = buildUser();
            ChangePasswordRequest request = new ChangePasswordRequest("wrong", "newPassword456");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.changePassword(1L, request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BadCredentialsException when the user does not exist")
        void shouldThrowBadCredentialsException_whenUserDoesNotExist() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("password123", "newPassword456");
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.changePassword(99L, request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }
    }
}