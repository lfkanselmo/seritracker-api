package com.seritracker.application.service;

import com.seritracker.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.AuthResponse;
import com.seritracker.infrastructure.adapter.out.persistence.JpaUserRepository;
import com.seritracker.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.seritracker.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JpaUserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();

        UserEntity saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(saved.getEmail())
                .name(saved.getName())
                .userId(saved.getId())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .userId(user.getId())
                .build();
    }
}