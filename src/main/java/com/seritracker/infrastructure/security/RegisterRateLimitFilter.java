package com.seritracker.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * A diferencia del login, acá cuenta todo intento de registro sin importar
 * el resultado — el objetivo es frenar creación masiva de cuentas, no dar
 * margen a que alguien reintente una contraseña olvidada.
 */
@Component
public class RegisterRateLimitFilter extends AbstractRateLimitFilter {

    private static final String REGISTER_PATH = "/api/v1/auth/register";

    private final RateLimiter rateLimiter;

    public RegisterRateLimitFilter(@Qualifier("registerRateLimiter") RateLimiter rateLimiter,
                                    ObjectMapper objectMapper) {
        super(objectMapper);
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean matches(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && REGISTER_PATH.equals(request.getRequestURI());
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
