package com.seritracker.domain.port.out;

public interface TokenService {
    String generateAccessToken(Long userId, String email, String role);
}
