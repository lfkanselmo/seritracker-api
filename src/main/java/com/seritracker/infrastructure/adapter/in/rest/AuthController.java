package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.domain.port.in.AuthUseCase;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ChangePasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ForgotPasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.RefreshTokenRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ResetPasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.AuthResponse;
import com.seritracker.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro e inicio de sesión")
public class AuthController {

    // Usamos el puerto, nunca el servicio directamente
    private final AuthUseCase authUseCase;

    @Operation(summary = "Registrar nuevo usuario")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.created(AuthResponse.from(
                authUseCase.register(request.getName(), request.getEmail(), request.getPassword())
        ));
    }

    @Operation(summary = "Iniciar sesión")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(AuthResponse.from(
                authUseCase.login(request.getEmail(), request.getPassword())
        ));
    }

    @Operation(summary = "Cambiar contraseña")
    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authUseCase.changePassword(principal.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.noContent("Password changed");
    }

    @Operation(summary = "Solicitar recuperación de contraseña por email")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authUseCase.forgotPassword(request.getEmail());
        return ApiResponse.noContent("If the email exists, a reset link has been sent");
    }

    @Operation(summary = "Restablecer contraseña con el token recibido por email")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authUseCase.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.noContent("Password reset successfully");
    }

    @Operation(summary = "Renovar el access token usando un refresh token")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(AuthResponse.from(authUseCase.refresh(request.getRefreshToken())));
    }

    @Operation(summary = "Cerrar sesión, invalidando el refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authUseCase.logout(request.getRefreshToken());
        return ApiResponse.noContent("Logged out");
    }
}
