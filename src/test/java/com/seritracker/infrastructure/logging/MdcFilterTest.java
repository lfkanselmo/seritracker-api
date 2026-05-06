package com.seritracker.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MdcFilter")
class MdcFilterTest {

    @Mock private HttpServletRequest  request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain         filterChain;

    private MdcFilter mdcFilter;

    @BeforeEach
    void setUp() {
        mdcFilter = new MdcFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("should add requestId to MDC when no header present")
    void shouldAddRequestId_whenNoHeaderPresent() throws Exception {
        when(request.getHeader("X-Request-ID")).thenReturn(null);

        mdcFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-Request-ID"), anyString());
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    @DisplayName("should use existing requestId from header")
    void shouldUseExistingRequestId_fromHeader() throws Exception {
        when(request.getHeader("X-Request-ID")).thenReturn("existing-id");

        mdcFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Request-ID", "existing-id");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should clear MDC after filter chain")
    void shouldClearMdc_afterFilterChain() throws Exception {
        when(request.getHeader("X-Request-ID")).thenReturn(null);

        mdcFilter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get("requestId")).isNull();
    }
}