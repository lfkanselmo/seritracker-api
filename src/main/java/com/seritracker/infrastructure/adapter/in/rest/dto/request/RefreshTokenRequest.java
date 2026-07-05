package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class RefreshTokenRequest {

    @NotBlank(message = "refresh token is required")
    String refreshToken;
}
