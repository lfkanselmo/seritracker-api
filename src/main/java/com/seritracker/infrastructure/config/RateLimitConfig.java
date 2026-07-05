package com.seritracker.infrastructure.config;

import com.seritracker.infrastructure.security.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter loginRateLimiter(
            @Value("${login.rate-limit.max-attempts}") int maxAttempts,
            @Value("${login.rate-limit.window-minutes}") long windowMinutes) {
        return new RateLimiter(maxAttempts, windowMinutes);
    }

    @Bean
    public RateLimiter registerRateLimiter(
            @Value("${register.rate-limit.max-attempts}") int maxAttempts,
            @Value("${register.rate-limit.window-minutes}") long windowMinutes) {
        return new RateLimiter(maxAttempts, windowMinutes);
    }
}
