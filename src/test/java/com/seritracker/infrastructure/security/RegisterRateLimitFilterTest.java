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
@DisplayName("RegisterRateLimitFilter")
class RegisterRateLimitFilterTest {

    @Mock private RateLimiter rateLimiter;
    @Mock private ObjectMapper objectMapper;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private RegisterRateLimitFilter filter;

    @Test
    @DisplayName("should pass through requests that are not register")
    void shouldPassThrough_whenNotRegisterRequest() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiter, never()).isBlocked(any());
    }

    @Test
    @DisplayName("should respond 429 and short-circuit when the IP is blocked")
    void shouldRespond429_whenIpIsBlocked() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isBlocked("1.2.3.4")).thenReturn(true);
        when(response.getWriter()).thenReturn(mock(java.io.PrintWriter.class));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("should record an attempt regardless of the response outcome (unlike login, it never checks status)")
    void shouldRecordAttempt_regardlessOfOutcome() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isBlocked("1.2.3.4")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiter).recordAttempt("1.2.3.4");
        verify(response, never()).getStatus();
    }
}
