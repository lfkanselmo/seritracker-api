package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.application.service.AuthService;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
}