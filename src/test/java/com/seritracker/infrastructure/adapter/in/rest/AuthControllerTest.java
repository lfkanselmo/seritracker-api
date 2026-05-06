package com.seritracker.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seritracker.application.service.AuthService;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.AuthResponse;
import com.seritracker.infrastructure.config.GlobalExceptionHandler;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
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
}