package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.domain.port.in.CheckUpcomingEpisodesUseCase;
import com.seritracker.domain.port.in.NotificationUseCase;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.NotificationResponse;
import com.seritracker.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notificaciones de nuevos episodios")
public class NotificationController {

    private final NotificationUseCase notificationUseCase;
    private final CheckUpcomingEpisodesUseCase checkUpcomingEpisodesUseCase;

    @Operation(summary = "Obtener notificaciones no leídas del usuario")
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getUnread(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(
                notificationUseCase.getUnreadNotifications(principal.getId())
                        .stream()
                        .map(NotificationResponse::from)
                        .toList()
        );
    }

    @Operation(summary = "Marcar notificación como leída")
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        notificationUseCase.markAsRead(principal.getId(), id);
        return ApiResponse.noContent();
    }

    @Operation(summary = "Disparar verificación de episodios manualmente")
    @PostMapping("/check")
    public ApiResponse<Void> triggerCheck() {
        checkUpcomingEpisodesUseCase.checkUpcomingEpisodes();
        return ApiResponse.noContent();
    }
}