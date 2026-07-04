package com.seritracker.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginRateLimitFilter")
class LoginRateLimitFilterTest {

    @Mock private LoginRateLimiter rateLimiter;
    @Mock private ObjectMapper objectMapper;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private LoginRateLimitFilter filter;

    @Test
    @DisplayName("should pass through requests that are not login")
    void shouldPassThrough_whenNotLoginRequest() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiter, never()).isBlocked(any());
    }

    @Test
    @DisplayName("should respond 429 and short-circuit when the IP is blocked")
    void shouldRespond429_whenIpIsBlocked() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isBlocked("1.2.3.4")).thenReturn(true);
        when(response.getWriter()).thenReturn(mock(java.io.PrintWriter.class));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("should record a failure when login responds 401")
    void shouldRecordFailure_whenLoginRespondsUnauthorized() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isBlocked("1.2.3.4")).thenReturn(false);
        when(response.getStatus()).thenReturn(HttpServletResponse.SC_UNAUTHORIZED);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiter).recordFailure("1.2.3.4");
        verify(rateLimiter, never()).recordSuccess(any());
    }

    @Test
    @DisplayName("should record a success when login responds 200")
    void shouldRecordSuccess_whenLoginRespondsOk() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isBlocked("1.2.3.4")).thenReturn(false);
        when(response.getStatus()).thenReturn(HttpServletResponse.SC_OK);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiter).recordSuccess("1.2.3.4");
        verify(rateLimiter, never()).recordFailure(any());
    }
}
