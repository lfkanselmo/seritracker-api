package com.seritracker.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {

    private final int maxAttempts;
    private final long windowMinutes;

    private final ConcurrentHashMap<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public RateLimiter(int maxAttempts, long windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.windowMinutes = windowMinutes;
    }

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

    public void recordAttempt(String key) {
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
