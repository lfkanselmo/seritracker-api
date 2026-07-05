package com.seritracker.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Sin esto, Spring Security usa Http403ForbiddenEntryPoint por defecto —
 * cualquier request sin JWT válido (incluido uno vencido) responde 403 en
 * vez de 401. Eso rompe la semántica REST (403 implica "sé quién sos pero
 * no podés", cuando en realidad no sabemos quién sos) y el frontend no
 * puede distinguir "sesión vencida, renovar" de "no tenés permiso".
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                          HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .data(null)
                .message("Authentication required")
                .timestamp(LocalDateTime.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
