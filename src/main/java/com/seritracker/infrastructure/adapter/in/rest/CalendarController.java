package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.domain.port.in.CalendarUseCase;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.UpcomingEpisodeResponse;
import com.seritracker.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Tag(name = "Calendar", description = "Calendario de próximos estrenos")
public class CalendarController {

    private final CalendarUseCase calendarUseCase;

    @Operation(summary = "Obtener los próximos episodios a estrenarse de las series en 'viendo'")
    @GetMapping("/upcoming")
    public ApiResponse<List<UpcomingEpisodeResponse>> getUpcoming(@AuthenticationPrincipal UserPrincipal principal) {
        List<UpcomingEpisodeResponse> response = calendarUseCase.getUpcomingEpisodes(principal.getId()).stream()
                .map(UpcomingEpisodeResponse::from)
                .toList();

        return ApiResponse.ok(response);
    }
}
