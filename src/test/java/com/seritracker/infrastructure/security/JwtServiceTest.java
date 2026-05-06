package com.seritracker.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET =
            "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
    private static final long TEST_EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",     TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", TEST_EXPIRATION);
    }

    @Test
    @DisplayName("should generate a non-null token")
    void shouldGenerateToken_notNull() {
        String token = jwtService.generateToken("user@test.com");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("should extract email from token correctly")
    void shouldExtractEmail_correctly() {
        String email = "user@test.com";
        String token = jwtService.generateToken(email);
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
    }

    @Test
    @DisplayName("should return true for valid token")
    void shouldReturnTrue_forValidToken() {
        String token = jwtService.generateToken("user@test.com");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("should return false for invalid token")
    void shouldReturnFalse_forInvalidToken() {
        assertThat(jwtService.isTokenValid("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("should return false for expired token")
    void shouldReturnFalse_forExpiredToken() {
        JwtService expiredService = new JwtService();
        ReflectionTestUtils.setField(expiredService, "secret",     TEST_SECRET);
        ReflectionTestUtils.setField(expiredService, "expiration", -1000L);

        String expiredToken = expiredService.generateToken("user@test.com");
        assertThat(expiredService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("should generate different tokens for different emails")
    void shouldGenerateDifferentTokens_forDifferentEmails() {
        String token1 = jwtService.generateToken("user1@test.com");
        String token2 = jwtService.generateToken("user2@test.com");
        assertThat(token1).isNotEqualTo(token2);
    }
}