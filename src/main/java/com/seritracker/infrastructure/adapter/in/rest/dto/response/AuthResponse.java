package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    String token;
    String email;
    String name;
    Long userId;
}