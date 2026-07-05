package com.seritracker.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Igual que el registro: cuenta todo intento sin importar el resultado.
 * Evita tanto spam de emails como usarlo para inferir qué direcciones
 * están registradas por temporización repetida.
 */
@Component
public class ForgotPasswordRateLimitFilter extends AbstractRateLimitFilter {

    private static final String FORGOT_PASSWORD_PATH = "/api/v1/auth/forgot-password";

    private final RateLimiter rateLimiter;

    public ForgotPasswordRateLimitFilter(@Qualifier("forgotPasswordRateLimiter") RateLimiter rateLimiter,
                                          ObjectMapper objectMapper) {
        super(objectMapper);
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean matches(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && FORGOT_PASSWORD_PATH.equals(request.getRequestURI());
    }

    @Override
    protected RateLimiter rateLimiter() {
        return rateLimiter;
    }

    @Override
    protected void onCompleted(HttpServletResponse response, String key) {
        rateLimiter.recordAttempt(key);
    }
}
