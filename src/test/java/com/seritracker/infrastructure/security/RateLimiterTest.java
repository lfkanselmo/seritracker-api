package com.seritracker.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimiter")
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(3, 15L);
    }

    @Test
    @DisplayName("should not block a key with no recorded attempts")
    void shouldNotBlock_whenNoAttemptsRecorded() {
        assertThat(rateLimiter.isBlocked("1.2.3.4")).isFalse();
    }

    @Test
    @DisplayName("should not block before reaching the max attempts")
    void shouldNotBlock_beforeReachingMaxAttempts() {
        rateLimiter.recordAttempt("1.2.3.4");
        rateLimiter.recordAttempt("1.2.3.4");

        assertThat(rateLimiter.isBlocked("1.2.3.4")).isFalse();
    }

    @Test
    @DisplayName("should block once max attempts is reached")
    void shouldBlock_onceMaxAttemptsIsReached() {
        rateLimiter.recordAttempt("1.2.3.4");
        rateLimiter.recordAttempt("1.2.3.4");
        rateLimiter.recordAttempt("1.2.3.4");

        assertThat(rateLimiter.isBlocked("1.2.3.4")).isTrue();
    }

    @Test
    @DisplayName("should track different keys independently")
    void shouldTrackDifferentKeys_independently() {
        rateLimiter.recordAttempt("1.2.3.4");
        rateLimiter.recordAttempt("1.2.3.4");
        rateLimiter.recordAttempt("1.2.3.4");

        assertThat(rateLimiter.isBlocked("1.2.3.4")).isTrue();
        assertThat(rateLimiter.isBlocked("5.6.7.8")).isFalse();
    }

    @Test
    @DisplayName("should reset attempts on success")
    void shouldResetAttempts_onSuccess() {
        rateLimiter.recordAttempt("1.2.3.4");
        rateLimiter.recordAttempt("1.2.3.4");
        rateLimiter.recordAttempt("1.2.3.4");
        assertThat(rateLimiter.isBlocked("1.2.3.4")).isTrue();

        rateLimiter.recordSuccess("1.2.3.4");

        assertThat(rateLimiter.isBlocked("1.2.3.4")).isFalse();
    }

    @Test
    @DisplayName("should not block once the window has expired")
    @SuppressWarnings("unchecked")
    void shouldNotBlock_onceWindowHasExpired() {
        rateLimiter.recordAttempt("1.2.3.4");
        rateLimiter.recordAttempt("1.2.3.4");
        rateLimiter.recordAttempt("1.2.3.4");
        assertThat(rateLimiter.isBlocked("1.2.3.4")).isTrue();

        // Retrocede el inicio de la ventana para simular que ya expiro, sin depender de tiempo real
        Map<String, Object> attemptsByKey =
                (Map<String, Object>) ReflectionTestUtils.getField(rateLimiter, "attemptsByKey");
        Object attempts = attemptsByKey.get("1.2.3.4");
        ReflectionTestUtils.setField(attempts, "windowStart", Instant.now().minus(Duration.ofMinutes(20)));

        assertThat(rateLimiter.isBlocked("1.2.3.4")).isFalse();
    }
}
