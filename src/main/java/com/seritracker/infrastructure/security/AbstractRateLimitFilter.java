package com.seritracker.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Esqueleto compartido por los filtros de rate limiting por IP: cada
 * subclase decide a qué ruta aplica y qué cuenta como intento una vez
 * pasada la request — el bloqueo y la respuesta 429 son siempre iguales.
 */
@Slf4j
public abstract class AbstractRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    protected AbstractRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected abstract boolean matches(HttpServletRequest request);

    protected abstract RateLimiter rateLimiter();

    protected abstract void onCompleted(HttpServletResponse response, String key);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        if (!matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request);

        if (rateLimiter().isBlocked(key)) {
            log.warn("Blocking request to {} from {} — rate limit exceeded", request.getRequestURI(), key);
            respondTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
        onCompleted(response, key);
    }

    protected String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void respondTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .data(null)
                .message("Too many attempts, please try again later")
                .timestamp(LocalDateTime.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
