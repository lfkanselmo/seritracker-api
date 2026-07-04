package com.seritracker.application.service;

import com.seritracker.domain.model.User;
import com.seritracker.domain.port.out.UserRepository;
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
            when(jwtService.generateToken(saved.getEmail())).thenReturn("jwt_token");

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
            when(jwtService.generateToken(anyString())).thenReturn("jwt_token");

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
            when(jwtService.generateToken(user.getEmail())).thenReturn("jwt_token");

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

            verify(jwtService, never()).generateToken(anyString());
        }
    }
}