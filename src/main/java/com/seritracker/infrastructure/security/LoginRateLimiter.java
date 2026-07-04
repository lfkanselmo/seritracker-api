package com.seritracker.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginRateLimiter {

    @Value("${login.rate-limit.max-attempts}")
    private int maxAttempts;

    @Value("${login.rate-limit.window-minutes}")
    private long windowMinutes;

    private final ConcurrentHashMap<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Attempts attempts = attemptsByKey.get(key);
        if (attempts == null) {
            return false;
        }
        if (attempts.isExpired(windowMinutes)) {
            attemptsByKey.remove(key, attempts);
            return false;
        }
        return attempts.count.get() >= maxAttempts;
    }

    public void recordFailure(String key) {
        attemptsByKey.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(windowMinutes)) {
                return new Attempts();
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(key);
    }

    private static class Attempts {
        final AtomicInteger count = new AtomicInteger(1);
        Instant windowStart = Instant.now();

        boolean isExpired(long windowMinutes) {
            return Instant.now().isAfter(windowStart.plus(Duration.ofMinutes(windowMinutes)));
        }
    }
}
