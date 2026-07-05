package com.seritracker.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seritracker.application.service.AuthService;
import com.seritracker.domain.exception.InvalidResetTokenException;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ChangePasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ForgotPasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ResetPasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.AuthResponse;
import com.seritracker.infrastructure.config.GlobalExceptionHandler;
import com.seritracker.infrastructure.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(1L, "test@test.com", "hashed_password", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
                .token("jwt_token")
                .email("user@test.com")
                .name("Test User")
                .userId(1L)
                .build();
    }

    // ── POST /api/v1/auth/register ─────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 when registration succeeds")
        void shouldReturn201_whenRegistrationSucceeds() throws Exception {
            RegisterRequest request = new RegisterRequest("Test User", "user@test.com", "password123");
            when(authService.register(any())).thenReturn(buildAuthResponse());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("jwt_token"))
                    .andExpect(jsonPath("$.data.userId").value(1));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400_whenNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"email\":\"user@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void shouldReturn400_whenEmailIsInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"email\":\"invalid\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void shouldReturn400_whenPasswordIsTooShort() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"email\":\"user@test.com\",\"password\":\"123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when email is already registered")
        void shouldReturn400_whenEmailIsAlreadyRegistered() throws Exception {
            RegisterRequest request = new RegisterRequest("Test", "user@test.com", "password123");
            when(authService.register(any()))
                    .thenThrow(new IllegalArgumentException("Email already registered"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }
    }

    // ── POST /api/v1/auth/login ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 when login succeeds")
        void shouldReturn200_whenLoginSucceeds() throws Exception {
            LoginRequest request = new LoginRequest("user@test.com", "password123");
            when(authService.login(any())).thenReturn(buildAuthResponse());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").value("jwt_token"));
        }

        @Test
        @DisplayName("should return 401 when credentials are invalid")
        void shouldReturn401_whenCredentialsAreInvalid() throws Exception {
            LoginRequest request = new LoginRequest("user@test.com", "wrong");
            when(authService.login(any()))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when email is blank")
        void shouldReturn400_whenEmailIsBlank() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── PATCH /api/v1/auth/password ────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/auth/password")
    class ChangePassword {

        @Test
        @DisplayName("should return 200 when the password is changed")
        void shouldReturn200_whenPasswordIsChanged() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("current123", "newPassword456");

            mockMvc.perform(patch("/api/v1/auth/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 401 when the current password is wrong")
        void shouldReturn401_whenCurrentPasswordIsWrong() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("wrong", "newPassword456");
            doThrow(new BadCredentialsException("Current password is incorrect"))
                    .when(authService).changePassword(any(), any());

            mockMvc.perform(patch("/api/v1/auth/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Current password is incorrect"));
        }

        @Test
        @DisplayName("should return 400 when the new password is too short")
        void shouldReturn400_whenNewPasswordIsTooShort() throws Exception {
            mockMvc.perform(patch("/api/v1/auth/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"current123\",\"newPassword\":\"123\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── POST /api/v1/auth/forgot-password ──────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("should return 200 when the email exists")
        void shouldReturn200_whenEmailExists() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest("user@test.com");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 200 even when the email does not exist (no enumeration)")
        void shouldReturn200_whenEmailDoesNotExist() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@test.com");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when the email is invalid")
        void shouldReturn400_whenEmailIsInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not-an-email\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── POST /api/v1/auth/reset-password ───────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("should return 200 when the token is valid")
        void shouldReturn200_whenTokenIsValid() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newPassword456");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 400 when the token is invalid or expired")
        void shouldReturn400_whenTokenIsInvalid() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("bad-token", "newPassword456");
            doThrow(new InvalidResetTokenException()).when(authService).resetPassword(any());

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));
        }

        @Test
        @DisplayName("should return 400 when the new password is too short")
        void shouldReturn400_whenNewPasswordIsTooShort() throws Exception {
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"some-token\",\"newPassword\":\"123\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}