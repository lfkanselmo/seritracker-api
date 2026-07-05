package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.application.service.AuthService;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ChangePasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.ForgotPasswordRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.LoginRequest;
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

    private final AuthService authService;

    @Operation(summary = "Registrar nuevo usuario")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.created(authService.register(request));
    }

    @Operation(summary = "Iniciar sesión")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "Cambiar contraseña")
    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
        return ApiResponse.noContent("Password changed");
    }

    @Operation(summary = "Solicitar recuperación de contraseña por email")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.noContent("If the email exists, a reset link has been sent");
    }

    @Operation(summary = "Restablecer contraseña con el token recibido por email")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.noContent("Password reset successfully");
    }
}