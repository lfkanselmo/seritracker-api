package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

@Value
@Builder
public class User {
    Long id;
    String email;
    String name;

    @With String passwordHash;

    String role;
    LocalDateTime createdAt;
}