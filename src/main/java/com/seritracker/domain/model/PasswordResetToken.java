package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PasswordResetToken {
    Long id;
    Long userId;
    String tokenHash;
    LocalDateTime expiresAt;
    LocalDateTime createdAt;
}
