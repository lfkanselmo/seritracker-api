package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.AuthResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    String accessToken;
    String refreshToken;
    String email;
    String name;
    Long userId;

    public static AuthResponse from(AuthResult domain) {
        return AuthResponse.builder()
                .accessToken(domain.getAccessToken())
                .refreshToken(domain.getRefreshToken())
                .email(domain.getEmail())
                .name(domain.getName())
                .userId(domain.getUserId())
                .build();
    }
}