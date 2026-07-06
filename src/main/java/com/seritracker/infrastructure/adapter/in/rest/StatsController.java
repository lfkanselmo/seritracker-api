package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.domain.port.in.StatsUseCase;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.UserStatsResponse;
import com.seritracker.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Estadísticas de uso del usuario")
public class StatsController {

    // Usamos el puerto, nunca el servicio directamente
    private final StatsUseCase statsUseCase;

    @Operation(summary = "Obtener las estadísticas del usuario (totales y resumen del año actual)")
    @GetMapping
    public ApiResponse<UserStatsResponse> getStats(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(UserStatsResponse.from(statsUseCase.getStats(principal.getId())));
    }
}
