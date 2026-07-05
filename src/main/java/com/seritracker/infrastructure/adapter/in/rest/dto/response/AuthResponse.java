package com.seritracker.infrastructure.adapter.in.rest.dto.response;

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
}