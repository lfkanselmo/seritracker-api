package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResult {
    Long userId;
    String name;
    String email;
    String accessToken;
    String refreshToken;
}
