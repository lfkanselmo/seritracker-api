package com.seritracker.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final LoginRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request);

        if (rateLimiter.isBlocked(key)) {
            log.warn("Blocking login attempt from {} — too many failed attempts", key);
            respondTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);

        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            rateLimiter.recordFailure(key);
        } else if (response.getStatus() == HttpServletResponse.SC_OK) {
            rateLimiter.recordSuccess(key);
        }
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI());
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void respondTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .data(null)
                .message("Too many login attempts, please try again later")
                .timestamp(LocalDateTime.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
