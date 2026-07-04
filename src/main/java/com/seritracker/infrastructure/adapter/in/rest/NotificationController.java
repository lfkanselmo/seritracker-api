package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.domain.model.Notification;
import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;
import com.seritracker.domain.port.in.CheckUpcomingEpisodesUseCase;
import com.seritracker.domain.port.in.NotificationUseCase;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.NotificationResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.PageResponse;
import com.seritracker.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notifications", description = "Notificaciones de nuevos episodios")
public class NotificationController {

    private final NotificationUseCase notificationUseCase;
    private final CheckUpcomingEpisodesUseCase checkUpcomingEpisodesUseCase;

    @Operation(summary = "Obtener notificaciones no leídas del usuario (paginado)")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getUnread(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {

        PageResult<Notification> result = notificationUseCase.getUnreadNotifications(
                principal.getId(), PageRequest.of(page, size));

        return ApiResponse.ok(PageResponse.from(result, NotificationResponse::from));
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
